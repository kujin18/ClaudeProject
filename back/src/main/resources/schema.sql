CREATE UNIQUE INDEX IF NOT EXISTS uk_short_link_active_account_prefix_alias
    ON short_link (account_id, domain_prefix, alias)
    WHERE deleted = false;
