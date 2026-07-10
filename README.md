# TaskFlow - Personal Productivity App

TaskFlow là ứng dụng Android hỗ trợ quản lý công việc cá nhân, project cá nhân, deadline, trạng thái tiến độ, ghi chú, comment và attachment. Ứng dụng hướng đến các cá nhân như học sinh, sinh viên, freelancer, developer hoặc người tự học cần quản lý nhiều đầu việc cùng lúc.

## 1. Đáp Ứng Yêu Cầu Kotlin Và Jetpack Compose

Ứng dụng được xây dựng bằng **Kotlin** và sử dụng **Jetpack Compose** cho toàn bộ giao diện người dùng.

Các màn hình chính của app đều được viết bằng Compose trong thư mục:

```text
app/src/main/java/com/taskflow/demo/presentation/
```

Ví dụ:

- `AuthScreens.kt`: màn hình đăng nhập và đăng ký
- `WorkspaceListScreen.kt`: màn hình danh sách workspace
- `ProjectBoardScreen.kt`: màn hình project board và task board
- `TaskDetailScreen.kt`: màn hình chi tiết task
- `DashboardScreen.kt`: màn hình thống kê tiến độ
- `ProfileScreen.kt`: màn hình hồ sơ người dùng

App **không sử dụng XML layout với Android View**. Project không có thư mục:

```text
app/src/main/res/layout/
```

Các file XML còn lại chỉ dùng cho cấu hình Android thông thường như `AndroidManifest.xml`, `strings.xml` và vector drawable logo. Chúng không phải XML layout View.

## 2. Đáp Ứng Yêu Cầu Room SQLite On-Device Database

Ứng dụng sử dụng **Room** để lưu và xử lý dữ liệu trực tiếp trên thiết bị bằng SQLite.

Database chính nằm tại:

```text
app/src/main/java/com/taskflow/demo/data/local/TaskFlowDatabase.kt
```

Room database hiện lưu các nhóm dữ liệu chính của app:

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

Các thao tác dữ liệu như `SELECT`, `INSERT`, `UPDATE`, `DELETE` được định nghĩa trong các DAO của Room, ví dụ:

- `LocalUserDao`
- `WorkspaceDao`
- `ProjectDao`
- `TaskDao`
- `TaskCommentDao`
- `TaskAttachmentDao`

Repository xử lý logic dữ liệu nằm tại:

```text
app/src/main/java/com/taskflow/demo/data/repository/Repositories.kt
```

Ở chế độ mặc định, app chạy với:

```properties
DATA_SOURCE_MODE=local
```

Điều này có nghĩa là dữ liệu chính của app được lưu và xử lý bằng **Room SQLite trên thiết bị**, không phụ thuộc backend bên ngoài.

App có giữ tùy chọn Supabase cho mục đích demo remote backend, nhưng Supabase chỉ là chế độ tùy chọn. Nó không thay thế yêu cầu Room của môn học.

## 3. Đáp Ứng Category Productivity App

TaskFlow thuộc nhóm **productivity app** vì mục tiêu chính là giúp người dùng quản lý công việc, deadline và tiến độ cá nhân.

Các use case phù hợp:

- Học sinh quản lý bài tập và deadline
- Sinh viên quản lý đồ án, môn học hoặc project cá nhân
- Freelancer quản lý task theo từng project
- Developer quản lý coding task, bug fix, feature và review
- Người tự học quản lý lộ trình học và mục tiêu cá nhân

App không liên quan đến chủ đề crypto currency.

## 4. Không Tập Trung Vào AI Features

Ứng dụng không sử dụng AI làm tính năng chính.

Trọng tâm của app là:

- Thiết kế giao diện mobile bằng Jetpack Compose
- Xây dựng nhiều màn hình có tính tương tác
- Lưu dữ liệu bằng Room SQLite
- Xử lý logic quản lý workspace, project, task và progress tracking

Điều này phù hợp với yêu cầu môn học là tập trung vào xây dựng mobile app UI và local database, không tập trung vào AI.

## 5. Số Lượng Màn Hình Và Thành Phần UI

App có nhiều màn hình và nhiều thành phần UI, không phải app chỉ có một hoặc hai màn hình đơn giản.

Các màn hình chính gồm:

- Splash screen
- Login screen
- Register screen
- Workspace list screen
- Create workspace dialog
- Project board screen
- Create project dialog
- Kanban task board
- Create task dialog
- Task detail screen
- Assignee editor
- Comment section
- Attachment section
- Dashboard screen
- Profile screen

Các thành phần UI nổi bật:

- Logo và nhận diện màu vàng TaskFlow
- Card workspace/project/task
- Form nhập liệu
- Dialog tạo dữ liệu
- Kanban board theo trạng thái
- Status chip
- Priority badge
- Progress summary
- Delete confirmation dialog
- Comment input
- Attachment picker
- Dashboard statistic cards

## Tính Năng Chính

- Đăng ký và đăng nhập tài khoản local
- Tạo workspace cá nhân
- Tạo project trong workspace
- Tạo task với tiêu đề, mô tả, deadline, priority và assignee
- Theo dõi task theo 4 trạng thái: To Do, In Progress, Review, Done
- Cập nhật trạng thái task
- Chỉnh assignee
- Thêm comment cho task
- Đính kèm file cho task
- Xóa task
- Xóa project và các task bên trong project
- Xóa workspace và các project/task liên quan
- Xem dashboard thống kê tiến độ
- Cập nhật profile cá nhân

## Kiến Trúc Ứng Dụng

Ứng dụng được tổ chức theo mô hình **MVVM kết hợp Repository Pattern**.

```text
Jetpack Compose UI
        |
ViewModel
        |
Repository
        |
Room SQLite Database
```

Các layer chính:

- `presentation`: chứa màn hình Compose và ViewModel
- `domain`: chứa model nghiệp vụ như `Workspace`, `Project`, `Task`, `TaskStatus`
- `data/local`: chứa Room entities, DAO và database
- `data/repository`: chứa logic xử lý dữ liệu
- `core/di`: cấu hình dependency injection bằng Hilt

## Cấu Hình Data Source

File cấu hình data source nằm tại:

```text
app/src/main/java/com/taskflow/demo/core/config/DataSourceConfig.kt
```

Mặc định app dùng local Room mode:

```properties
DATA_SOURCE_MODE=local
```

Nếu muốn chạy thử Supabase mode, có thể tạo `local.properties` từ file mẫu:

```bash
cp local.properties.example local.properties
```

Sau đó cấu hình:

```properties
DATA_SOURCE_MODE=supabase
SUPABASE_URL=replace_with_your_supabase_project_url
SUPABASE_ANON_KEY=replace_with_your_anon_public_key
```

Lưu ý: bản nộp coursework nên giữ `DATA_SOURCE_MODE=local` để đảm bảo dữ liệu chính được lưu bằng Room SQLite trên thiết bị.

## Build APK

Từ thư mục gốc của project:

```bash
./gradlew :app:assembleDebug
```

APK debug được tạo tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Nếu muốn đổi tên file APK:

```bash
cp app/build/outputs/apk/debug/app-debug.apk app_tracking_task.apk
```
