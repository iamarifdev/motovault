-- REVIEW ONLY. Not executed or connected to any project.
-- Ciphertext is supplied only by a future reviewed authenticated-encryption adapter.
-- No names, plates, OCR values, local paths or raw attachments belong here.
begin;
create table public.motovault_snapshots (
    owner_id uuid not null references auth.users(id),
    vault_id uuid not null,
    remote_revision bigint not null check (remote_revision > 0),
    operation_id text not null,
    key_version integer not null check (key_version > 0),
    ciphertext bytea not null,
    primary key (owner_id, vault_id)
);
create table public.motovault_operations (
    owner_id uuid not null references auth.users(id),
    vault_id uuid not null,
    operation_id text not null,
    remote_revision bigint not null check (remote_revision > 0),
    ciphertext_digest bytea not null,
    primary key (owner_id, vault_id, operation_id)
);
alter table public.motovault_snapshots enable row level security;
alter table public.motovault_operations enable row level security;
revoke all on public.motovault_snapshots, public.motovault_operations from anon, authenticated;
grant select on public.motovault_snapshots, public.motovault_operations to authenticated;
create policy motovault_read_snapshot on public.motovault_snapshots for select to authenticated
using ((select auth.uid()) = owner_id);
create policy motovault_read_operations on public.motovault_operations for select to authenticated
using ((select auth.uid()) = owner_id);
-- Intentionally no client table writes: a future owner-checked transaction/RPC
-- must lock the snapshot, check expected revision, deduplicate operation IDs,
-- and write operation receipt and snapshot atomically. No last-write-wins upsert.

insert into storage.buckets(id, name, public, file_size_limit, allowed_mime_types)
values ('motovault-encrypted', 'motovault-encrypted', false, 67108864, array['application/octet-stream']);
-- Immutable object names: <authenticated owner UUID>/<vault UUID>/<random object UUID>.
-- INSERT only, no overwrite/upsert; deletion needs reviewed manifest/receipt GC.
create policy motovault_read_ciphertext on storage.objects for select to authenticated
using (bucket_id = 'motovault-encrypted' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy motovault_insert_ciphertext on storage.objects for insert to authenticated
with check (bucket_id = 'motovault-encrypted' and (storage.foldername(name))[1] = (select auth.uid())::text);
commit;
