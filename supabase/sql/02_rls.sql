-- Row Level Security policies for Android anon-key access.

ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workspaces ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workspace_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.task_labels ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.task_label_map ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.task_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.task_attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.task_activity_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION public.is_workspace_member(target_workspace UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.workspace_members wm
    WHERE wm.workspace_id = target_workspace
      AND wm.user_id = auth.uid()
  );
$$;

CREATE OR REPLACE FUNCTION public.is_project_member(target_project UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.projects p
    JOIN public.workspace_members wm ON wm.workspace_id = p.workspace_id
    WHERE p.project_id = target_project
      AND wm.user_id = auth.uid()
  );
$$;

CREATE OR REPLACE FUNCTION public.owns_workspace(target_workspace UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.workspaces w
    WHERE w.workspace_id = target_workspace
      AND w.owner_id = auth.uid()
  );
$$;

DROP POLICY IF EXISTS "Users read profiles in shared workspaces" ON public.user_profiles;
CREATE POLICY "Users read profiles in shared workspaces" ON public.user_profiles
  FOR SELECT USING (
    user_id = auth.uid()
    OR EXISTS (
      SELECT 1
      FROM public.workspace_members mine
      JOIN public.workspace_members other_member ON other_member.workspace_id = mine.workspace_id
      WHERE mine.user_id = auth.uid()
        AND other_member.user_id = user_profiles.user_id
    )
  );

DROP POLICY IF EXISTS "Users update own profile" ON public.user_profiles;
CREATE POLICY "Users update own profile" ON public.user_profiles
  FOR UPDATE USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "Users insert own profile" ON public.user_profiles;
CREATE POLICY "Users insert own profile" ON public.user_profiles
  FOR INSERT WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "Members read workspaces" ON public.workspaces;
CREATE POLICY "Members read workspaces" ON public.workspaces
  FOR SELECT USING (public.is_workspace_member(workspace_id));

DROP POLICY IF EXISTS "Users create owned workspaces" ON public.workspaces;
CREATE POLICY "Users create owned workspaces" ON public.workspaces
  FOR INSERT WITH CHECK (owner_id = auth.uid());

DROP POLICY IF EXISTS "Owners and admins update workspaces" ON public.workspaces;
CREATE POLICY "Owners and admins update workspaces" ON public.workspaces
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM public.workspace_members wm
      WHERE wm.workspace_id = workspaces.workspace_id
        AND wm.user_id = auth.uid()
        AND wm.role IN ('owner', 'admin')
    )
  );

DROP POLICY IF EXISTS "Owners delete workspaces" ON public.workspaces;
CREATE POLICY "Owners delete workspaces" ON public.workspaces
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM public.workspace_members wm
      WHERE wm.workspace_id = workspaces.workspace_id
        AND wm.user_id = auth.uid()
        AND wm.role = 'owner'
    )
  );

DROP POLICY IF EXISTS "Members read workspace memberships" ON public.workspace_members;
CREATE POLICY "Members read workspace memberships" ON public.workspace_members
  FOR SELECT USING (public.is_workspace_member(workspace_id));

DROP POLICY IF EXISTS "Users create own owner membership" ON public.workspace_members;
CREATE POLICY "Users create own owner membership" ON public.workspace_members
  FOR INSERT WITH CHECK (
    user_id = auth.uid()
    AND role = 'owner'
    AND public.owns_workspace(workspace_id)
  );

DROP POLICY IF EXISTS "Members read projects" ON public.projects;
CREATE POLICY "Members read projects" ON public.projects
  FOR SELECT USING (public.is_workspace_member(workspace_id));

DROP POLICY IF EXISTS "Members create projects" ON public.projects;
CREATE POLICY "Members create projects" ON public.projects
  FOR INSERT WITH CHECK (public.is_workspace_member(workspace_id));

DROP POLICY IF EXISTS "Members update projects" ON public.projects;
CREATE POLICY "Members update projects" ON public.projects
  FOR UPDATE USING (public.is_workspace_member(workspace_id))
  WITH CHECK (public.is_workspace_member(workspace_id));

DROP POLICY IF EXISTS "Owners and admins delete projects" ON public.projects;
CREATE POLICY "Owners and admins delete projects" ON public.projects
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM public.workspace_members wm
      WHERE wm.workspace_id = projects.workspace_id
        AND wm.user_id = auth.uid()
        AND wm.role IN ('owner', 'admin')
    )
  );

DROP POLICY IF EXISTS "Members manage tasks" ON public.tasks;
CREATE POLICY "Members manage tasks" ON public.tasks
  FOR ALL USING (public.is_project_member(project_id))
  WITH CHECK (public.is_project_member(project_id));

DROP POLICY IF EXISTS "Members read labels" ON public.task_labels;
CREATE POLICY "Members read labels" ON public.task_labels
  FOR SELECT USING (public.is_workspace_member(workspace_id));

DROP POLICY IF EXISTS "Members create labels" ON public.task_labels;
CREATE POLICY "Members create labels" ON public.task_labels
  FOR INSERT WITH CHECK (public.is_workspace_member(workspace_id));

DROP POLICY IF EXISTS "Members read task label map" ON public.task_label_map;
CREATE POLICY "Members read task label map" ON public.task_label_map
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM public.tasks t
      WHERE t.task_id = task_label_map.task_id
        AND public.is_project_member(t.project_id)
    )
  );

DROP POLICY IF EXISTS "Members manage comments" ON public.task_comments;
CREATE POLICY "Members manage comments" ON public.task_comments
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM public.tasks t
      WHERE t.task_id = task_comments.task_id
        AND public.is_project_member(t.project_id)
    )
  )
  WITH CHECK (
    user_id = auth.uid()
    AND EXISTS (
      SELECT 1 FROM public.tasks t
      WHERE t.task_id = task_comments.task_id
        AND public.is_project_member(t.project_id)
    )
  );

DROP POLICY IF EXISTS "Members manage attachments" ON public.task_attachments;
CREATE POLICY "Members manage attachments" ON public.task_attachments
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM public.tasks t
      WHERE t.task_id = task_attachments.task_id
        AND public.is_project_member(t.project_id)
    )
  )
  WITH CHECK (
    uploaded_by = auth.uid()
    AND EXISTS (
      SELECT 1 FROM public.tasks t
      WHERE t.task_id = task_attachments.task_id
        AND public.is_project_member(t.project_id)
    )
  );

DROP POLICY IF EXISTS "Members read activity logs" ON public.task_activity_logs;
CREATE POLICY "Members read activity logs" ON public.task_activity_logs
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM public.tasks t
      WHERE t.task_id = task_activity_logs.task_id
        AND public.is_project_member(t.project_id)
    )
  );

DROP POLICY IF EXISTS "Users read own notifications" ON public.notifications;
CREATE POLICY "Users read own notifications" ON public.notifications
  FOR SELECT USING (user_id = auth.uid());

DROP POLICY IF EXISTS "Users update own notifications" ON public.notifications;
CREATE POLICY "Users update own notifications" ON public.notifications
  FOR UPDATE USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());
