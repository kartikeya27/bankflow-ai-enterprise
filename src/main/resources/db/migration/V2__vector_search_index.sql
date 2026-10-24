-- HNSW cosine index. With only five policies a sequential scan is also fine.
CREATE INDEX IF NOT EXISTS idx_policy_hnsw ON policy_documents
USING hnsw (embedding vector_cosine_ops);
