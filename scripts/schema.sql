DROP TABLE IF EXISTS "snapshots" CASCADE;
DROP TABLE IF EXISTS "tagSeenEvents" CASCADE;
DROP TABLE IF EXISTS "events" CASCADE;

CREATE TABLE "events" (
	"id" bigserial PRIMARY KEY,
	"type" text NOT NULL,
	"time" timestamp NOT NULL,
	"data" text NOT NULL,
	"removed" boolean NOT NULL
);

CREATE UNIQUE INDEX ON "events" ("time" DESC, "id" DESC);
CREATE INDEX ON "events" ("type", "removed");

CREATE TABLE "tagSeenEvents" (
	"id" bigserial PRIMARY KEY REFERENCES "events" ("id"),
	"readerId" integer NOT NULL,
	"updateCount" bigint NOT NULL
);

CREATE UNIQUE INDEX ON "tagSeenEvents" ("readerId", "updateCount" DESC);

CREATE TABLE "snapshots" (
	"id" bigserial PRIMARY KEY,
	"time" timestamp NOT NULL,
	"data" text NOT NULL,
	"event" bigint REFERENCES "events" ("id") NOT NULL
);

CREATE INDEX ON "snapshots" ("time" DESC);
