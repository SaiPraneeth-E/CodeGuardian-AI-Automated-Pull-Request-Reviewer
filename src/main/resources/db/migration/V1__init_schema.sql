-- Create Pull Requests table
CREATE TABLE IF NOT EXISTS pull_requests (
    id UUID PRIMARY KEY,
    github_pr_id BIGINT UNIQUE NOT NULL,
    pr_number INT NOT NULL,
    repository_name VARCHAR(255) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    title VARCHAR(512),
    state VARCHAR(50) NOT NULL,
    head_sha VARCHAR(100) NOT NULL,
    base_sha VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create Reviews table
CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY,
    pull_request_id UUID NOT NULL REFERENCES pull_requests(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    summary TEXT,
    overall_score INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- Create Comments table
CREATE TABLE IF NOT EXISTS comments (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    file_path VARCHAR(512) NOT NULL,
    line_number INT,
    agent_type VARCHAR(50) NOT NULL,
    comment_text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_pull_requests_github_pr_id ON pull_requests(github_pr_id);
CREATE INDEX IF NOT EXISTS idx_pull_requests_repo_pr ON pull_requests(repository_name, pr_number);
CREATE INDEX IF NOT EXISTS idx_reviews_pr_id ON reviews(pull_request_id);
CREATE INDEX IF NOT EXISTS idx_comments_review_id ON comments(review_id);
