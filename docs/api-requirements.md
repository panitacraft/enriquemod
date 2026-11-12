# Requirements for panita-api, from the Minecraft mod

Written for the agent that works on `panita-api`. The API runs on Vercel and is billed by Fluid CPU time, so the mod is being made to call it as little as possible. Two changes in the API would cut that further and fix one gap. All three are for the routes the mod already uses with the `mod` service key.

## 1. `GET /v1/mod/players` must also list members who never chose `minecraft_status`

Today only enabled users that have an IGN **and** a `minecraft_status` are listed. Old members with an active account and an IGN, who never chose premium or cracked, are left out, so they are not whitelisted.

Required:

- List every enabled user that has an IGN, whether or not they chose a status.
- For a member who did not choose, send `"minecraft_status": null` (the key present). Keep `"premium"` / `"cracked"` for the rest. No other value.
- Everything else stays as documented (`user_id`, `ign`, `discord_id`, the ETag and `If-None-Match` handling).

The mod decides the mode itself for those members: an entry the whitelist already has is kept, otherwise it asks Mojang (a Microsoft account with that name means premium, no account means cracked).

Compatibility: a mod build that does not know about `null` rejects the whole list as malformed and changes nothing (it fails safe, it does not remove anyone). The new mod build ships first, then this change is deployed.

## 2. A bulk route for the mod profiles

Today the mod reads one profile per request: `GET /v1/mod/players/{user_id}/profile?edition_id=`. A reload or `/profile refresh all` makes one request per online player.

Required: `GET /v1/mod/profiles?edition_id=<id>`

- Returns `{ "data": [ <profile>, ... ] }` for every enabled user that has an IGN, where `<profile>` is exactly the object the single route returns in `data` (same fields, same emblem filtering and ordering). A user the single route would answer 404 for is simply not in the list.
- Same authentication as the other `/v1/mod` routes.
- Sends an `ETag` and answers `If-None-Match` with `304`, as `/v1/mod/players` does.

The mod will ask this once at startup/reload/refresh-all instead of once per player, and keep the single route only for one player's own refresh.

## 3. Make the answers cheap to produce

Whatever the mod asks again and again should cost almost nothing:

- Compute the ETag from something cheap, such as the latest `updated_at` over the rows involved (and the edition id), so a `304` does not build the body at all.
- Send `Cache-Control: private, max-age=0, must-revalidate` together with the ETag so the intent is explicit.
- Prefer a single query for the bulk route (emblems of all users in one go) over one query per user.

## What the mod already does on its side

- Whitelist list: conditional request with the stored ETag, only on start, reload, `/link sync` and a few rare events. No timer.
- Profiles: cached in the world folder for `profile.cacheSeconds` (30 minutes by default), and kept across restarts. `link.autoFetch = false` turns every automatic read off (test server).
