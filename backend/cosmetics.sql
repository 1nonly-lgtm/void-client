-- Void Client 2.0: kosmetyki zapisane na koncie (UUID Minecrafta). Wklej w Supabase → SQL Editor → Run.
-- Każdy widzi kosmetyki innych graczy Void Client, zmienić można tylko swoje (token Minecrafta sprawdza void_whoami).

create table if not exists public.void_cosmetics (
  uuid uuid primary key,
  data jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);
alter table public.void_cosmetics enable row level security;

-- Zapis własnych kosmetyków (z launchera). Dane: {cape, wings, hat, pet, fx, emoteSlots[24]} – maks. 4 KB.
create or replace function public.void_cosmetics_set(p_token text, p_data jsonb)
returns void
language plpgsql security definer
set search_path = public, extensions
as $$
declare me public.void_tokens;
begin
  me := public.void_whoami(p_token);
  if p_data is null or length(p_data::text) > 4096 then
    raise exception 'Za dużo danych';
  end if;
  insert into public.void_cosmetics(uuid, data, updated_at) values (me.uuid, p_data, now())
    on conflict (uuid) do update set data = excluded.data, updated_at = now();
end $$;

-- Kosmetyki podanych graczy (gra pyta o graczy z listy Tab). Maks. 200 naraz.
create or replace function public.void_cosmetics_get(p_uuids uuid[])
returns table(uuid uuid, data jsonb)
language sql stable security definer
set search_path = public
as $$
  select c.uuid, c.data from public.void_cosmetics c where c.uuid = any(p_uuids[1:200]);
$$;

revoke all on function public.void_cosmetics_set(text, jsonb) from public;
revoke all on function public.void_cosmetics_get(uuid[]) from public;
grant execute on function public.void_cosmetics_set(text, jsonb) to anon, authenticated;
grant execute on function public.void_cosmetics_get(uuid[]) to anon, authenticated;

-- Emotki widoczne dla innych graczy Void: gra wysyła, co teraz grasz (null = koniec), inni pytają co 2 s.
create table if not exists public.void_emotes (
  uuid uuid primary key,
  emote text,
  started_at timestamptz not null default now()
);
alter table public.void_emotes enable row level security;

create or replace function public.void_emote_set(p_token text, p_emote text)
returns void
language plpgsql security definer
set search_path = public, extensions
as $$
declare me public.void_tokens;
begin
  me := public.void_whoami(p_token);
  if p_emote is not null and p_emote !~ '^[a-z]{2,24}$' then
    raise exception 'Zła emotka';
  end if;
  insert into public.void_emotes(uuid, emote, started_at) values (me.uuid, p_emote, now())
    on conflict (uuid) do update set emote = excluded.emote, started_at = now();
end $$;

create or replace function public.void_emote_get(p_uuids uuid[])
returns table(uuid uuid, emote text, age_ms bigint)
language sql stable security definer
set search_path = public
as $$
  select e.uuid, e.emote, (extract(epoch from (now() - e.started_at)) * 1000)::bigint
  from public.void_emotes e
  where e.uuid = any(p_uuids[1:200]) and e.emote is not null and e.started_at > now() - interval '10 minutes';
$$;

revoke all on function public.void_emote_set(text, text) from public;
revoke all on function public.void_emote_get(uuid[]) from public;
grant execute on function public.void_emote_set(text, text) to anon, authenticated;
grant execute on function public.void_emote_get(uuid[]) to anon, authenticated;
