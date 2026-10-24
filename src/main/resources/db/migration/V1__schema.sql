CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE bank_cases (
 id UUID PRIMARY KEY,
 reference VARCHAR(40) NOT NULL UNIQUE,
 input_json JSONB NOT NULL,
 case_json JSONB NOT NULL,
 status VARCHAR(40) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_case_created ON bank_cases (created_at DESC);
CREATE TABLE policy_documents (
 id VARCHAR(30) PRIMARY KEY,
 version INTEGER NOT NULL,
 title TEXT NOT NULL,
 body TEXT NOT NULL,
 embedding_provider VARCHAR(100),
 embedding vector(1024),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE audit_events (
 seq BIGSERIAL PRIMARY KEY,
 case_id UUID NOT NULL REFERENCES bank_cases(id),
 actor TEXT NOT NULL,
 action TEXT NOT NULL,
 detail TEXT NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL,
 previous_hash VARCHAR(64) NOT NULL,
 event_hash VARCHAR(64) NOT NULL
);
CREATE INDEX idx_audit_case ON audit_events(case_id,seq);
CREATE OR REPLACE FUNCTION reject_audit_mutation() RETURNS TRIGGER AS $$
BEGIN
  RAISE EXCEPTION 'audit_events is append-only';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER audit_no_update BEFORE UPDATE OR DELETE ON audit_events
  FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();
