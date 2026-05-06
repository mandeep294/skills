---
name: ops-da
description: Document Authoring (DA) admin operations - list content, get/put source, copy, move, delete, versioning, and configuration via admin.da.live API.
allowed-tools: Read, Write, Edit, Bash
---

# Edge Delivery Services Operations - Document Authoring (DA)

Content management operations for Document Authoring repository (`admin.da.live`).

## Prerequisites

**IMS Token Required** - Uses the same IMS token as all other operations.

If token is missing, invoke the auth skill first:
```
Skill({ skill: "project-management:auth" })
```

---

## API Reference

Base URL: `https://admin.da.live`

| Intent | Endpoint | Method | Description |
|--------|----------|--------|-------------|
| List orgs | `/list` | GET | List available DA organizations |
| List content | `/list/{org}/{site}/{path}` | GET | List files/folders |
| Get source | `/source/{org}/{site}/{path}` | GET | Get file content |
| Create/update | `/source/{org}/{site}/{path}` | POST/PUT | Upload content |
| Delete | `/source/{org}/{site}/{path}` | DELETE | Delete file(s) |
| Copy | `/copy/{org}/{site}/{path}` | POST | Copy file/folder |
| Move | `/move/{org}/{site}/{path}` | POST | Move/rename |
| Get config | `/config/{org}/{site}` | GET | Get DA site config |
| Set config | `/config/{org}/{site}` | POST | Update DA site config |
| List versions | `/versionlist/{org}/{site}/{path}` | GET | List file versions |
| Get version | `/versionsource/{versionId}` | GET | Get version content |
| Restore version | `/versionsource/{org}/{site}/{path}` | POST | Restore a version |

---

## Operations

### List Organizations

Show all DA organizations the user has access to.

```bash
curl -s --connect-timeout 15 --max-time 120 \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  "https://admin.da.live/list"
```

**Response:**
```json
[
  {"name": "myorg", "created": "2024-01-15T10:30:00Z"},
  {"name": "another-org", "created": "2024-02-20T14:00:00Z"}
]
```

### List Content

List files and folders in a DA path.

```bash
# List site root
curl -s --connect-timeout 15 --max-time 120 \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  "https://admin.da.live/list/${ORG}/${SITE}"

# List specific folder
curl -s --connect-timeout 15 --max-time 120 \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  "https://admin.da.live/list/${ORG}/${SITE}/blog"
```

**Response:**
```json
{
  "children": [
    {"name": "index.html", "path": "/mysite/index.html", "lastModified": "2024-03-15T09:00:00Z"},
    {"name": "about.html", "path": "/mysite/about.html", "lastModified": "2024-03-14T15:30:00Z"},
    {"name": "blog", "path": "/mysite/blog/", "isFolder": true}
  ]
}
```

**Present as table:**
| Name | Type | Last Modified |
|------|------|---------------|
| index.html | file | 2024-03-15 09:00 |
| about.html | file | 2024-03-14 15:30 |
| blog/ | folder | - |

### Get Source Content

Retrieve file content from DA.

```bash
curl -s --connect-timeout 15 --max-time 120 \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  "https://admin.da.live/source/${ORG}/${SITE}/${PATH}"
```

For HTML files, response is the HTML content. For media, response is binary.

### Create/Update Content

Upload or update content in DA.

```bash
# Upload HTML content
curl -s --connect-timeout 15 --max-time 120 -X POST \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  -H "Content-Type: text/html" \
  --data-binary "@content.html" \
  "https://admin.da.live/source/${ORG}/${SITE}/${PATH}"

# Upload from string
curl -s --connect-timeout 15 --max-time 120 -X POST \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  -H "Content-Type: text/html" \
  -d '<html><body><h1>Hello</h1></body></html>' \
  "https://admin.da.live/source/${ORG}/${SITE}/hello.html"

# Upload media (form data)
curl -s --connect-timeout 15 --max-time 120 -X POST \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  -F "file=@image.png" \
  "https://admin.da.live/source/${ORG}/${SITE}/media/image.png"
```

**Success:** HTTP 200 (update) or 201 (create)

### Delete Content

**DESTRUCTIVE OPERATION - CONFIRMATION REQUIRED**

Before executing:
1. State: "This will permanently delete {path} from Document Authoring."
2. Ask: "Do you want to proceed? (yes/no)"
3. Only execute if user confirms "yes"

```bash
# Delete single file
curl -s --connect-timeout 15 --max-time 120 -X DELETE \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  "https://admin.da.live/source/${ORG}/${SITE}/${PATH}"

# Delete multiple files
curl -s --connect-timeout 15 --max-time 120 -X DELETE \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"paths": ["/page1.html", "/page2.html"]}' \
  "https://admin.da.live/source/${ORG}/${SITE}"
```

### Copy Content

Copy files or folders within DA.

```bash
curl -s --connect-timeout 15 --max-time 120 -X POST \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"destination\": \"/${ORG}/${SITE}/${DEST_PATH}\"}" \
  "https://admin.da.live/copy/${ORG}/${SITE}/${SOURCE_PATH}"
```

**Example:** Copy template to new page
```bash
curl -s --connect-timeout 15 --max-time 120 -X POST \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"destination": "/myorg/mysite/new-page.html"}' \
  "https://admin.da.live/copy/myorg/mysite/templates/basic.html"
```

### Move/Rename Content

Move or rename files/folders.

```bash
curl -s --connect-timeout 15 --max-time 120 -X POST \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"destination\": \"/${ORG}/${SITE}/${DEST_PATH}\"}" \
  "https://admin.da.live/move/${ORG}/${SITE}/${SOURCE_PATH}"
```

**Example:** Rename file
```bash
curl -s --connect-timeout 15 --max-time 120 -X POST \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"destination": "/myorg/mysite/new-name.html"}' \
  "https://admin.da.live/move/myorg/mysite/old-name.html"
```

---

## Versioning Operations

### List Versions

```bash
curl -s --connect-timeout 15 --max-time 120 \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  "https://admin.da.live/versionlist/${ORG}/${SITE}/${PATH}"
```

**Response:**
```json
{
  "versions": [
    {"id": "abc123", "created": "2024-03-15T10:00:00Z", "author": "user@example.com"},
    {"id": "def456", "created": "2024-03-14T09:00:00Z", "author": "user@example.com"}
  ]
}
```

### Get Version Content

```bash
curl -s --connect-timeout 15 --max-time 120 \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  "https://admin.da.live/versionsource/${VERSION_ID}"
```

### Restore Version

```bash
curl -s --connect-timeout 15 --max-time 120 -X POST \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"versionId\": \"${VERSION_ID}\"}" \
  "https://admin.da.live/versionsource/${ORG}/${SITE}/${PATH}"
```

---

## Configuration

### Get DA Site Config

```bash
curl -s --connect-timeout 15 --max-time 120 \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  "https://admin.da.live/config/${ORG}/${SITE}"
```

### Update DA Site Config

```bash
curl -s --connect-timeout 15 --max-time 120 -X POST \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"key": "value"}' \
  "https://admin.da.live/config/${ORG}/${SITE}"
```

---

## Preview/Publish DA Content

After modifying content in DA, trigger Edge Delivery Services preview/publish:

```bash
# Preview DA content
curl -s --connect-timeout 15 --max-time 120 -X POST \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  -H "x-content-source-authorization: Bearer ${IMS_TOKEN}" \
  "https://admin.hlx.page/preview/${ORG}/${SITE}/${REF}/${PATH}"

# Publish DA content
curl -s --connect-timeout 15 --max-time 120 -X POST \
  -H "Authorization: Bearer ${IMS_TOKEN}" \
  -H "x-content-source-authorization: Bearer ${IMS_TOKEN}" \
  "https://admin.hlx.page/live/${ORG}/${SITE}/${REF}/${PATH}"
```

**Note:** The `x-content-source-authorization` header passes the IMS token to authorize DA content source access.

---

## Natural Language Patterns

| User Says | Operation |
|-----------|-----------|
| "da list" | List orgs |
| "da list files in /blog" | List content |
| "da show /index.html" | Get source |
| "da get source /about" | Get source |
| "da upload content to /new.html" | Create content |
| "da update /page.html" | Update content |
| "da delete /old.html" | Delete (confirm) |
| "da copy /template to /new" | Copy |
| "da move /draft to /final" | Move |
| "da rename /old to /new" | Move |
| "da versions /page.html" | List versions |
| "da restore version X" | Restore version |
| "da config" | Get site config |
| "da preview /page" | Preview after DA edit |
| "da publish /page" | Publish after DA edit |

---

## Error Handling

| HTTP Code | Cause | Action |
|-----------|-------|--------|
| **401** | Missing/invalid IMS token | Run `Skill({ skill: "project-management:auth" })` |
| **403** | No permission for org/site/path | Check DA access permissions |
| **404** | Path not found | Verify org, site, path exist |
| **409** | Conflict (concurrent edit) | Retry or resolve version conflict |
| **413** | File too large | Check DA file size limits |
| **500** | Server error | Retry; check DA service status |

---

## DA vs SharePoint/Google Drive

| Feature | DA | SharePoint/Drive |
|---------|-----|------------------|
| Auth | IMS OAuth token | Config Service token |
| Admin API | admin.da.live | admin.hlx.page only |
| Bulk ops | API with explicit paths | Wildcard `/*` supported |
| Versioning | Built-in `/versionlist` | Via content source |
| Direct edit | Yes (via API) | Edit in source app |

**Key difference for preview/publish:**
- DA sites: Use `Authorization` + `x-content-source-authorization` headers
- SharePoint/Drive: Use `Authorization: Bearer` header
