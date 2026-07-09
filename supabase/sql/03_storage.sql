-- Storage buckets and policies for demo attachments.

INSERT INTO storage.buckets (id, name, public)
VALUES
  ('avatars', 'avatars', true),
  ('task-attachments', 'task-attachments', true)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "Authenticated users upload avatars" ON storage.objects;
CREATE POLICY "Authenticated users upload avatars" ON storage.objects
  FOR INSERT TO authenticated
  WITH CHECK (bucket_id = 'avatars');

DROP POLICY IF EXISTS "Public read avatars" ON storage.objects;
CREATE POLICY "Public read avatars" ON storage.objects
  FOR SELECT USING (bucket_id = 'avatars');

DROP POLICY IF EXISTS "Authenticated users upload task attachments" ON storage.objects;
CREATE POLICY "Authenticated users upload task attachments" ON storage.objects
  FOR INSERT TO authenticated
  WITH CHECK (bucket_id = 'task-attachments');

DROP POLICY IF EXISTS "Authenticated users read task attachments" ON storage.objects;
CREATE POLICY "Authenticated users read task attachments" ON storage.objects
  FOR SELECT TO authenticated
  USING (bucket_id = 'task-attachments');
