# Vault ACL-политика sbp-router-management (применяется downstream-пайплайном tkbpay/vault-config).
# KV v1 mount `pay`, раскладка pay/{env}/{app}-{secret} (без сегмента data/).
# Свои секреты: pay/test/sbp-router-management-db-password
path "pay/test/sbp-router-management-*" {
  capabilities = ["read"]
}

# callee: ключ-идентичность caller'а payadmin-bff (валидация X-Internal-Admin-Key).
path "pay/test/payadmin-bff-internal-key" {
  capabilities = ["read"]
}

# transitional (dual-accept) — старый общий admin-key. Убрать после cutover на per-caller.
path "pay/test/sbp-router-admin-key" {
  capabilities = ["read"]
}
