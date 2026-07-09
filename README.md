# TaskFlow

TaskFlow is a personal productivity Android app for managing workspaces, projects, tasks, deadlines, progress, comments, and attachments. The app is designed mainly for individual users such as students, freelancers, developers, and self-learners.

## Features

- Splash, login, and registration screens
- Personal workspace management
- Project creation and project board
- Kanban task board with four statuses: To Do, In Progress, Review, Done
- Fixed task status colors:
  - To Do: smoke gray
  - In Progress: blue
  - Review: orange
  - Done: green
- Task creation with priority, due date, and assignee
- Task detail with comments and attachments
- Delete task, project, and workspace with related data cleanup
- Dashboard for progress tracking
- Profile screen

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- MVVM architecture
- Hilt dependency injection
- Room SQLite database for on-device data storage
- Optional Supabase mode for remote backend demo

## Architecture

The app follows MVVM with a repository layer.

```text
Jetpack Compose UI
        |
ViewModel
        |
Repository
        |
Room SQLite database by default
```

The app uses a configurable data source mode:

- `local`: stores and processes core app data on-device with Room SQLite.
- `supabase`: uses Supabase for remote backend demo.

By default, the app uses:

```properties
DATA_SOURCE_MODE=local
```

This keeps the app aligned with coursework requirements that require on-device SQLite storage using Room.

## Room Database

Room stores the main app data locally, including:

- Local users
- User profiles
- Workspaces
- Workspace members
- Projects
- Tasks
- Task comments
- Task attachments
- Task drafts
- Search history
- Cached projects

Room schema files are exported under:

```text
app/schemas/
```

## Optional Supabase Configuration

Supabase is optional and should not replace the Room-first local mode for coursework submission.

Create a `local.properties` file from the example:

```bash
cp local.properties.example local.properties
```

For local Room mode:

```properties
DATA_SOURCE_MODE=local
```

For Supabase mode:

```properties
DATA_SOURCE_MODE=supabase
SUPABASE_URL=replace_with_your_supabase_project_url
SUPABASE_ANON_KEY=replace_with_your_anon_public_key
```

Do not commit `local.properties`, database passwords, or service role keys.

## Build

From the project root:

```bash
./gradlew :app:assembleDebug
```

The debug APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- The UI is built with Jetpack Compose.
- The project does not use XML layout files with Android Views.
- Crypto currency topics are not part of this app.
- AI features are not required for this app.
