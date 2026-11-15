package com.readsake.dev.repo.dao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
@Repository
public class CatalogLookupCacheDaoImpl implements CatalogLookupCacheDao {
    @Autowired private NamedParameterJdbcTemplate jdbc;
    @Override public Optional<Entry> findFresh(String lookupKey) {
        List<Entry> rows = jdbc.query("SELECT response::text AS response FROM catalog_lookup_cache WHERE lookup_key=:key AND expires_at > NOW()",
                new MapSqlParameterSource("key", lookupKey), (rs, i) -> new Entry(rs.getString("response")));
        return rows.stream().findFirst();
    }
    @Override public void upsert(String lookupKey, String json, Duration ttl) {
        String sql = "INSERT INTO catalog_lookup_cache (lookup_key,response,fetched_at,expires_at) VALUES (:key,CAST(:json AS jsonb),NOW(),NOW()+make_interval(secs => :ttl)) ON CONFLICT (lookup_key) DO UPDATE SET response=EXCLUDED.response,fetched_at=NOW(),expires_at=EXCLUDED.expires_at";
        jdbc.update(sql, new MapSqlParameterSource().addValue("key", lookupKey).addValue("json", json).addValue("ttl", ttl.toSeconds()));
    }
}
