-- Allow a routing config to be re-published as a new version even if its sha256
-- checksum equals a previously-published manifest (e.g. rolling config back to a
-- prior shape). `version` remains the manifest identity (version_uk); `checksum`
-- is a content hash that may legitimately recur across versions.
alter table routing_manifests drop constraint if exists routing_manifests_checksum_uk;
