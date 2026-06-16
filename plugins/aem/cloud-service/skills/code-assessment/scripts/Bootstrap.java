import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Bootstrap — compile-cache + run entry point for the code-assessment analyzer, in pure Java.
 *
 * Run it directly with the JDK 11+ single-file source launcher (no pre-compile step):
 *
 *   java <scripts>/Bootstrap.java <workspace-root> [--pattern <slug>] [--files a,b] [--all] [--list-patterns]
 *
 * The skill ships the analyzer as source (reviewable, no binary in the repo), and the analyzer
 * already needs the system Java compiler at runtime (it parses the workspace with JavacTask), so
 * this bootstrap reuses that same compiler to build the analyzer — no extra dependency, and no
 * shell. Behaviour matches the former analyze.sh: hash the analyzer sources, compile once to a
 * hashed temp cache, reuse the cache when sources are unchanged, then invoke analyzer.Analyze.
 * Nothing is written to the project tree; the cache lives in the system temp dir.
 *
 * Exit codes: 0 ran · 2 usage (from Analyze) · 3 no JDK compiler · 4 unknown --pattern (from
 * Analyze) · 5 analyzer failed to compile · 6 cannot locate analyzer sources.
 */
public class Bootstrap {

    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("error: JDK required (system Java compiler not found) — a JRE alone cannot run this");
            System.exit(3);
        }

        Path srcRoot = locateAnalyzerSrc();   // <Bootstrap.java dir>/analyzer
        if (srcRoot == null) {
            System.err.println("error: cannot locate analyzer sources — run: java <scripts>/Bootstrap.java <workspace-root> [flags]");
            System.exit(6);
        }

        List<Path> sources = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(srcRoot)) {
            walk.filter(p -> p.toString().endsWith(".java")).sorted().forEach(sources::add);
        }
        if (sources.isEmpty()) {
            System.err.println("error: no analyzer sources under " + srcRoot);
            System.exit(6);
        }

        Path cache = Paths.get(System.getProperty("java.io.tmpdir"), "aem-code-assessment", hash(sources));
        Path ok = cache.resolve(".ok");
        if (!Files.isRegularFile(ok)) {
            compileToCache(compiler, sources, cache);   // exits 5 on failure
            Files.write(ok, new byte[0]);
        }

        runAnalyzer(cache, args);
    }

    /**
     * The analyzer source root is the {@code analyzer/} directory sitting next to this Bootstrap.java.
     * The JDK single-file source launcher records the launched source path in the
     * {@code jdk.launcher.sourcefile} system property — a clean absolute path, regardless of the
     * caller's working directory or path spaces. Falls back to {@code analyzer/} under the working
     * directory (e.g. when run from the scripts dir itself).
     */
    private static Path locateAnalyzerSrc() {
        String sourceFile = System.getProperty("jdk.launcher.sourcefile");
        if (sourceFile != null && !sourceFile.isEmpty()) {
            Path self = Paths.get(sourceFile).toAbsolutePath().normalize();
            Path candidate = self.getParent().resolve("analyzer");
            if (Files.isDirectory(candidate)) return candidate;
        }
        Path fallback = Paths.get(System.getProperty("user.dir")).resolve("analyzer");
        return Files.isDirectory(fallback) ? fallback : null;
    }

    /** SHA-256 over each source's relative path + bytes — the cache key; changes when any source changes. */
    private static String hash(List<Path> sources) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        for (Path p : sources) {
            md.update(p.toAbsolutePath().normalize().toString().getBytes("UTF-8"));
            md.update((byte) 0);
            md.update(Files.readAllBytes(p));
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** javac -d <cache> <sources...> via the in-process compiler; mirrors the former analyze.sh. */
    private static void compileToCache(JavaCompiler compiler, List<Path> sources, Path cache) throws Exception {
        deleteRecursively(cache);
        Files.createDirectories(cache);
        List<String> argv = new ArrayList<>();
        argv.add("-d");
        argv.add(cache.toString());
        for (Path p : sources) argv.add(p.toString());
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int rc = compiler.run(null, null, err, argv.toArray(new String[0]));
        if (rc != 0) {
            System.err.println("error: analyzer failed to compile");
            System.err.print(err.toString("UTF-8"));
            System.exit(5);
        }
    }

    /** Load the compiled analyzer from the cache and invoke analyzer.Analyze.main(args). */
    private static void runAnalyzer(Path cache, String[] args) throws Exception {
        URL[] urls = { cache.toUri().toURL() };
        try (URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader())) {
            Class<?> main = Class.forName("analyzer.Analyze", true, loader);
            Method m = main.getMethod("main", String[].class);
            // Analyze.main writes JSON to stdout and may System.exit(2|4); that propagates as our exit code.
            m.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.err.println("error: analyzer run failed: " + cause);
            System.exit(1);
        }
    }

    private static void deleteRecursively(Path dir) throws Exception {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (Exception ignored) { }
            });
        }
    }
}
