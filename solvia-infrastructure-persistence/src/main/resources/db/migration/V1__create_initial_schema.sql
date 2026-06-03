create table accounts (
    id uuid primary key,
    name text not null,
    type text not null,
    envelope_type text not null,
    currency_code char(3) not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    constraint accounts_name_not_blank check (btrim(name) <> ''),
    constraint accounts_type_check check (type in (
        'CHECKING',
        'SAVINGS',
        'INVESTMENT',
        'PEA',
        'CTO',
        'CRYPTO_EXCHANGE',
        'CRYPTO_WALLET',
        'CASHBACK',
        'OTHER'
    )),
    constraint accounts_envelope_type_check check (envelope_type in (
        'CURRENT_ACCOUNT',
        'REGULATED_SAVINGS',
        'PEA',
        'CTO',
        'CRYPTO',
        'PRIVATE_ASSET',
        'CASHBACK',
        'OTHER'
    )),
    constraint accounts_currency_code_format check (currency_code ~ '^[A-Z]{3}$')
);

create table assets (
    id uuid primary key,
    name text not null,
    type text not null,
    currency_code char(3) not null,
    symbol text,
    active boolean not null,
    created_at timestamp with time zone not null,
    constraint assets_name_not_blank check (btrim(name) <> ''),
    constraint assets_type_check check (type in (
        'FIAT_CURRENCY',
        'STOCK',
        'ETF',
        'BOND',
        'CRYPTO_ASSET',
        'PRIVATE_EQUITY',
        'REAL_ESTATE',
        'CASHBACK_REWARD',
        'OTHER'
    )),
    constraint assets_currency_code_format check (currency_code ~ '^[A-Z]{3}$')
);

create table positions (
    id uuid primary key,
    account_id uuid not null references accounts(id),
    asset_id uuid not null references assets(id),
    quantity numeric(38, 18) not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    constraint positions_quantity_not_negative check (quantity >= 0),
    constraint positions_account_asset_unique unique (account_id, asset_id)
);

create table account_balance_snapshots (
    id uuid primary key,
    account_id uuid not null references accounts(id),
    value_date date not null,
    amount numeric(38, 18) not null,
    currency_code char(3) not null,
    confidence text not null,
    note text,
    recorded_at timestamp with time zone not null,
    constraint account_balance_snapshots_currency_code_format check (currency_code ~ '^[A-Z]{3}$'),
    constraint account_balance_snapshots_confidence_check check (confidence in (
        'OBSERVED',
        'ESTIMATED_HIGH',
        'ESTIMATED_MEDIUM',
        'ESTIMATED_LOW'
    ))
);

create table position_snapshots (
    id uuid primary key,
    position_id uuid not null references positions(id),
    value_date date not null,
    quantity numeric(38, 18) not null,
    market_value_amount numeric(38, 18) not null,
    market_value_currency_code char(3) not null,
    confidence text not null,
    recorded_at timestamp with time zone not null,
    constraint position_snapshots_quantity_not_negative check (quantity >= 0),
    constraint position_snapshots_market_value_not_negative check (market_value_amount >= 0),
    constraint position_snapshots_currency_code_format check (market_value_currency_code ~ '^[A-Z]{3}$'),
    constraint position_snapshots_confidence_check check (confidence in (
        'OBSERVED',
        'ESTIMATED_HIGH',
        'ESTIMATED_MEDIUM',
        'ESTIMATED_LOW'
    ))
);

create table cash_flows (
    id uuid primary key,
    account_id uuid not null references accounts(id),
    type text not null,
    value_date date not null,
    amount numeric(38, 18) not null,
    currency_code char(3) not null,
    label text,
    recorded_at timestamp with time zone not null,
    constraint cash_flows_type_check check (type in (
        'DEPOSIT',
        'WITHDRAWAL',
        'TRANSFER_IN',
        'TRANSFER_OUT',
        'INTEREST',
        'DIVIDEND',
        'FEE',
        'TAX',
        'CASHBACK',
        'CORRECTION'
    )),
    constraint cash_flows_amount_not_zero check (amount <> 0),
    constraint cash_flows_currency_code_format check (currency_code ~ '^[A-Z]{3}$')
);

create table market_prices (
    id uuid primary key,
    asset_id uuid not null references assets(id),
    price_date date not null,
    price_amount numeric(38, 18) not null,
    price_currency_code char(3) not null,
    source text,
    recorded_at timestamp with time zone not null,
    constraint market_prices_amount_positive check (price_amount > 0),
    constraint market_prices_currency_code_format check (price_currency_code ~ '^[A-Z]{3}$')
);

create table fx_rates (
    id uuid primary key,
    base_currency_code char(3) not null,
    quote_currency_code char(3) not null,
    rate_date date not null,
    rate numeric(38, 18) not null,
    source text,
    recorded_at timestamp with time zone not null,
    constraint fx_rates_base_currency_code_format check (base_currency_code ~ '^[A-Z]{3}$'),
    constraint fx_rates_quote_currency_code_format check (quote_currency_code ~ '^[A-Z]{3}$'),
    constraint fx_rates_currencies_different check (base_currency_code <> quote_currency_code),
    constraint fx_rates_rate_positive check (rate > 0)
);

create table import_batches (
    id uuid primary key,
    format text not null,
    source_name text,
    imported_at timestamp with time zone not null,
    item_count integer not null,
    status text not null,
    error_message text,
    constraint import_batches_item_count_not_negative check (item_count >= 0),
    constraint import_batches_format_check check (format in ('CSV', 'JSON', 'BACKUP', 'MANUAL')),
    constraint import_batches_status_check check (status in ('CREATED', 'IMPORTED', 'FAILED', 'ROLLED_BACK'))
);

create table audit_logs (
    id uuid primary key,
    occurred_at timestamp with time zone not null,
    actor text not null,
    action text not null,
    entity_type text not null,
    entity_id uuid,
    details_json jsonb not null default '{}'::jsonb,
    constraint audit_logs_actor_not_blank check (btrim(actor) <> ''),
    constraint audit_logs_action_not_blank check (btrim(action) <> ''),
    constraint audit_logs_entity_type_not_blank check (btrim(entity_type) <> '')
);

create index idx_account_balance_snapshots_account_date on account_balance_snapshots(account_id, value_date desc);
create index idx_position_snapshots_position_date on position_snapshots(position_id, value_date desc);
create index idx_cash_flows_account_date on cash_flows(account_id, value_date desc);
create index idx_market_prices_asset_date on market_prices(asset_id, price_date desc);
create index idx_fx_rates_pair_date on fx_rates(base_currency_code, quote_currency_code, rate_date desc);
create index idx_import_batches_imported_at on import_batches(imported_at desc);
create index idx_audit_logs_occurred_at on audit_logs(occurred_at desc);
create index idx_audit_logs_entity on audit_logs(entity_type, entity_id);
