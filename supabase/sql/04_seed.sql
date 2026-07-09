-- Demo seed. Replace <USER_ID> with a real auth.users.id from Supabase Auth.

INSERT INTO public.workspaces (workspace_id, name, description, owner_id)
VALUES (
  '00000000-0000-0000-0000-000000000001',
  'Demo Workspace',
  'Workspace mẫu để demo quản lý task',
  '<USER_ID>'
)
ON CONFLICT (workspace_id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    owner_id = EXCLUDED.owner_id;

INSERT INTO public.workspace_members (workspace_id, user_id, role)
VALUES ('00000000-0000-0000-0000-000000000001', '<USER_ID>', 'owner')
ON CONFLICT (workspace_id, user_id) DO UPDATE SET role = EXCLUDED.role;

INSERT INTO public.projects (project_id, workspace_id, name, description, status, color_hex, due_date)
VALUES (
  '00000000-0000-0000-0000-000000000101',
  '00000000-0000-0000-0000-000000000001',
  'Mobile App MVP',
  'Project mẫu cho bản demo TaskFlow',
  'active',
  '#2563EB',
  CURRENT_DATE + INTERVAL '14 days'
)
ON CONFLICT (project_id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    color_hex = EXCLUDED.color_hex,
    due_date = EXCLUDED.due_date;

INSERT INTO public.task_labels (workspace_id, name, color_hex) VALUES
  ('00000000-0000-0000-0000-000000000001', 'Frontend', '#60A5FA'),
  ('00000000-0000-0000-0000-000000000001', 'Backend',  '#34D399'),
  ('00000000-0000-0000-0000-000000000001', 'Bug',      '#F87171')
ON CONFLICT (workspace_id, name) DO UPDATE SET color_hex = EXCLUDED.color_hex;

INSERT INTO public.tasks (task_id, project_id, title, description, status, priority, reporter_id, assignee_id, due_at, sort_order)
VALUES
  ('00000000-0000-0000-0000-000000001001', '00000000-0000-0000-0000-000000000101', 'Design Login Flow',
   'Tạo UI đăng nhập/đăng ký bằng Jetpack Compose', 'todo', 'high', '<USER_ID>', '<USER_ID>', NOW() + INTERVAL '2 days', 1),
  ('00000000-0000-0000-0000-000000001002', '00000000-0000-0000-0000-000000000101', 'Setup Supabase Schema',
   'Tạo bảng workspace, project, task, comment, attachment', 'in_progress', 'urgent', '<USER_ID>', '<USER_ID>', NOW() + INTERVAL '1 day', 2),
  ('00000000-0000-0000-0000-000000001003', '00000000-0000-0000-0000-000000000101', 'Build Task Detail Screen',
   'Hiển thị thông tin task, comment, attachment', 'review', 'medium', '<USER_ID>', '<USER_ID>', NOW() + INTERVAL '5 days', 3),
  ('00000000-0000-0000-0000-000000001004', '00000000-0000-0000-0000-000000000101', 'Prepare Demo Script',
   'Chuẩn bị kịch bản demo cho stakeholder', 'done', 'low', '<USER_ID>', '<USER_ID>', NOW() - INTERVAL '1 day', 4)
ON CONFLICT (task_id) DO UPDATE
SET title = EXCLUDED.title,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    priority = EXCLUDED.priority,
    reporter_id = EXCLUDED.reporter_id,
    assignee_id = EXCLUDED.assignee_id,
    due_at = EXCLUDED.due_at,
    sort_order = EXCLUDED.sort_order;
