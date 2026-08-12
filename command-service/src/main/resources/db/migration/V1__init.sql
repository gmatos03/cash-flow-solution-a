-- Solution A shared schema (Appendix F.6).
-- Command Service owns event_store; the same script is bundled with the
-- other three services so any one of them can bootstrap a fresh database.
-- Flyway's schema_history table (shared across services pointed at the same
-- database) makes re-running this a safe no-op once one service has applied it.

CREATE TABLE IF NOT EXISTS event_store (
  event_id        UUID PRIMARY KEY,
  entry_id        VARCHAR(40)  NOT NULL,
  account_id      VARCHAR(40)  NOT NULL,
  type            VARCHAR(10)  NOT NULL CHECK (type IN ('CREDIT','DEBIT')),
  amount          NUMERIC(18,2) NOT NULL CHECK (amount > 0),
  currency        CHAR(3)      NOT NULL,
  channel         VARCHAR(20)  NOT NULL,
  description     VARCHAR(140),
  occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  schema_version  SMALLINT     NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_event_store_account_occurred
  ON event_store (account_id, occurred_at);

CREATE TABLE IF NOT EXISTS accounts (
  account_id       VARCHAR(40)  PRIMARY KEY,
  account_name     VARCHAR(120) NOT NULL,
  currency         CHAR(3)      NOT NULL,
  opening_balance  NUMERIC(18,2) NOT NULL,
  current_balance  NUMERIC(18,2) NOT NULL,
  updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ledger_entries (
  entry_id     VARCHAR(40) PRIMARY KEY,
  account_id   VARCHAR(40) NOT NULL REFERENCES accounts(account_id),
  type         VARCHAR(10) NOT NULL,
  amount       NUMERIC(18,2) NOT NULL,
  currency     CHAR(3) NOT NULL,
  channel      VARCHAR(20) NOT NULL,
  description  VARCHAR(140),
  posted_at    TIMESTAMPTZ NOT NULL,
  status       VARCHAR(10) NOT NULL DEFAULT 'POSTED'
               CHECK (status IN ('POSTED','FLAGGED'))
);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_account_posted
  ON ledger_entries (account_id, posted_at DESC);

CREATE TABLE IF NOT EXISTS daily_cash_flow_log (
  account_id             VARCHAR(40) NOT NULL REFERENCES accounts(account_id),
  report_date            DATE        NOT NULL,
  opening_balance        NUMERIC(18,2) NOT NULL,
  total_credits          NUMERIC(18,2) NOT NULL,
  total_debits           NUMERIC(18,2) NOT NULL,
  closing_balance        NUMERIC(18,2) NOT NULL,
  reconciliation_status  VARCHAR(15) NOT NULL DEFAULT 'PENDING',
  generated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (account_id, report_date)
);

-- A couple of seed accounts so the sample curl requests in the README work
-- out of the box against a freshly created database.
INSERT INTO accounts (account_id, account_name, currency, opening_balance, current_balance)
VALUES
  ('acc-10293847', 'Operating Account - Acme Corp', 'USD', 46963.55, 46963.55),
  ('acc-55510023', 'Payroll Account - Acme Corp',   'USD', 12000.00, 12000.00)
ON CONFLICT (account_id) DO NOTHING;
