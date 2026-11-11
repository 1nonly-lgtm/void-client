-- Void Client: gracze, znajomi i ikonka Void przy nicku.
-- Do tabel nie ma bezpośredniego dostępu (RLS bez reguł) – wszystko idzie przez funkcje poniżej,
-- które same sprawdzają token Minecraft gracza u Mojanga (nikt nie podszyje się pod cudzy nick).

create extension if not exists http with schema extensions;
create extension if not exists pgcrypto with schema extensions;

create table if not exists public.void_players (
  uuid uuid primary key,
  name text not null,
  last_seen timestamptz not null default now(),
  playing boolean not null default false,
  created_at timestamptz not null default now()
);
create index if not exists void_players_name_idx on public.void_players (lower(name));

create table if not exists public.void_tokens (
  hash text primary key,
  uuid uuid not null,
  name text not null,
  checked_at timestamptz not null default now()
);

create table if not exists public.void_friends (
  owner uuid not null references public.void_players(uuid) on delete cascade,
  friend uuid not null references public.void_players(uuid) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (owner, friend),
  check (owner <> friend)
);

alter table public.void_players enable row level security;
alter table public.void_tokens enable row level security;
alter table public.void_friends enable row level security;

-- Kim jest gracz z tym tokenem? Pyta api.minecraftservices.com, wynik pamięta 6 h (hash tokenu, nie token).
create or replace function public.void_whoami(p_token text)
returns public.void_tokens
language plpgsql security definer
set search_path = public, extensions
as $$
declare
  h text;
  t public.void_tokens;
  r extensions.http_response;
  j jsonb;
begin
  if p_token is null or length(p_token) < 20 then
    raise exception 'Brak logowania Minecraft';
  end if;
  h := encode(extensions.digest(p_token, 'sha256'), 'hex');
  select * into t from public.void_tokens where hash = h and checked_at > now() - interval '6 hours';
  if not found then
    perform extensions.http_set_curlopt('CURLOPT_TIMEOUT', '8');
    select * into r from extensions.http((
      'GET'::extensions.http_method,
      'https://api.minecraftservices.com/minecraft/profile',
      array[extensions.http_header('Authorization', 'Bearer ' || p_token)],
      null, null)::extensions.http_request);
    if r.status <> 200 then
      raise exception 'Nieprawidłowe logowanie Minecraft (%)', r.status;
    end if;
    j := r.content::jsonb;
    insert into public.void_tokens(hash, uuid, name, checked_at)
      values (h, (j->>'id')::uuid, j->>'name', now())
      on conflict (hash) do update set uuid = excluded.uuid, name = excluded.name, checked_at = now()
      returning * into t;
    delete from public.void_tokens where checked_at < now() - interval '2 days';
  end if;
  insert into public.void_players(uuid, name) values (t.uuid, t.name)
    on conflict (uuid) do update set name = excluded.name;
  return t;
end $$;

-- Co 1–2 min z launchera (playing = false) i z gry (playing = true).
create or replace function public.void_heartbeat(p_token text, p_playing boolean default false)
returns jsonb
language plpgsql security definer
set search_path = public, extensions
as $$
declare me public.void_tokens;
begin
  me := public.void_whoami(p_token);
  update public.void_players set last_seen = now(), playing = p_playing where uuid = me.uuid;
  return jsonb_build_object('uuid', me.uuid, 'name', me.name);
end $$;

-- Którzy z podanych graczy używają Void Client (ikonka przy nicku). Maks. 200 naraz.
create or replace function public.void_users(p_uuids uuid[])
returns setof uuid
language sql stable security definer
set search_path = public
as $$
  select p.uuid from public.void_players p where p.uuid = any(p_uuids[1:200]);
$$;

-- Lista znajomych: friend = obie strony się dodały, sent = wysłane zaproszenie, request = ktoś Cię zaprosił.
-- Status online widać tylko u wzajemnych znajomych.
create or replace function public.void_friends(p_token text)
returns table(uuid uuid, name text, status text, online boolean, playing boolean)
language plpgsql security definer
set search_path = public, extensions
as $$
#variable_conflict use_column
declare me public.void_tokens;
begin
  me := public.void_whoami(p_token);
  return query
  select p.uuid, p.name,
    case when o.friend is not null and i.owner is not null then 'friend'
         when o.friend is not null then 'sent' else 'request' end,
    (o.friend is not null and i.owner is not null and p.last_seen > now() - interval '3 minutes'),
    (o.friend is not null and i.owner is not null and p.playing and p.last_seen > now() - interval '3 minutes')
  from public.void_players p
  left join public.void_friends o on o.owner = me.uuid and o.friend = p.uuid
  left join public.void_friends i on i.owner = p.uuid and i.friend = me.uuid
  where o.friend is not null or i.owner is not null
  order by 4 desc, 2;
end $$;

create or replace function public.void_friend_add(p_token text, p_name text)
returns text
language plpgsql security definer
set search_path = public, extensions
as $$
declare me public.void_tokens; f uuid; n text;
begin
  me := public.void_whoami(p_token);
  select p.uuid, p.name into f, n from public.void_players p where lower(p.name) = lower(trim(p_name)) limit 1;
  if f is null then raise exception 'Gracz % nie używa Void Client', trim(p_name); end if;
  if f = me.uuid then raise exception 'Nie możesz dodać samego siebie'; end if;
  insert into public.void_friends(owner, friend) values (me.uuid, f) on conflict do nothing;
  return n;
end $$;

create or replace function public.void_friend_remove(p_token text, p_uuid uuid)
returns void
language plpgsql security definer
set search_path = public, extensions
as $$
declare me public.void_tokens;
begin
  me := public.void_whoami(p_token);
  delete from public.void_friends where (owner = me.uuid and friend = p_uuid) or (owner = p_uuid and friend = me.uuid);
end $$;

revoke all on function public.void_whoami(text) from public, anon, authenticated;
revoke all on function public.void_heartbeat(text, boolean) from public;
revoke all on function public.void_users(uuid[]) from public;
revoke all on function public.void_friends(text) from public;
revoke all on function public.void_friend_add(text, text) from public;
revoke all on function public.void_friend_remove(text, uuid) from public;
grant execute on function public.void_heartbeat(text, boolean) to anon, authenticated;
grant execute on function public.void_users(uuid[]) to anon, authenticated;
grant execute on function public.void_friends(text) to anon, authenticated;
grant execute on function public.void_friend_add(text, text) to anon, authenticated;
grant execute on function public.void_friend_remove(text, uuid) to anon, authenticated;
