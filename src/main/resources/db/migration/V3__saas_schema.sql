-- Create Users Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    github_user_id BIGINT UNIQUE NOT NULL,
    login VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255),
    avatar_url VARCHAR(512),
    oauth_token_encrypted TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create User Connected Repositories Table
CREATE TABLE IF NOT EXISTS user_connected_repositories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    github_repo_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    webhook_id BIGINT,
    webhook_secret VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_repo UNIQUE (user_id, github_repo_id)
);

-- Update Pull Requests Table
ALTER TABLE pull_requests ADD COLUMN IF NOT EXISTS repository_id UUID REFERENCES user_connected_repositories(id) ON DELETE CASCADE;

-- Create Indexes
CREATE INDEX IF NOT EXISTS idx_users_github_id ON users(github_user_id);
CREATE INDEX IF NOT EXISTS idx_user_repos_user_id ON user_connected_repositories(user_id);
CREATE INDEX IF NOT EXISTS idx_pull_requests_repo_id ON pull_requests(repository_id);
