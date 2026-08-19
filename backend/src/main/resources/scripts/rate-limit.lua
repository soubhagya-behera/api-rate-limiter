-- rate-limit.lua
-- Atomic fixed-window rate limiter.
--
-- The whole script runs as a single atomic step inside Redis, so concurrent
-- requests for the same key can never observe or create a partial state.
-- There is no separate increment + expire; the decision, the increment and
-- the window creation all happen in one uninterruptible script.
--
-- KEYS[1] - the rate-limit counter key for a client (prefix + client IP)
-- ARGV[1] - maxRequests   (the configured request limit per window)
-- ARGV[2] - windowSeconds (the fixed window length)
--
-- Returns a 4-element array:
--   [1] count     - the current counter value after this request
--   [2] ttl       - remaining TTL of the window in seconds (-1 if none)
--   [3] allowed   - 1 if the request is allowed, 0 if it must be rejected
--   [4] remaining - requests still available in this window (never negative)

local key = KEYS[1]
local maxRequests = tonumber(ARGV[1])
local windowSeconds = tonumber(ARGV[2])

-- Read the current counter. A missing or already-expired key counts as 0,
-- which starts a brand-new window.
local count = tonumber(redis.call('GET', key) or '0')
local allowed = 0

if count < maxRequests then
    -- The request fits inside the window, so increment the counter.
    count = redis.call('INCR', key)

    -- The very first request of a window sets the expiration so the whole
    -- window is associated with a single TTL. Later requests, including
    -- rejected ones, never touch the TTL.
    if count == 1 then
        redis.call('EXPIRE', key, windowSeconds)
    end

    allowed = 1
end

-- When the limit has already been reached we deliberately do NOT increment.
-- The counter therefore stays capped at maxRequests and the TTL is left
-- untouched, so rejected requests neither grow the counter nor reset the
-- window.

local ttl = redis.call('TTL', key)
local remaining = maxRequests - count

return { count, ttl, allowed, remaining }