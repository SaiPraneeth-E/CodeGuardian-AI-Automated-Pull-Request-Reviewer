import { useState, useEffect, useCallback } from 'react'

const API_BASE = '/api'

function AgentCard({ name, rate, icon }) {
  const barColor = rate >= 80 ? '#22c55e' : rate >= 60 ? '#f59e0b' : '#ef4444'
  return (
    <div className="agent-card">
      <div className="agent-header">
        <span className="agent-icon">{icon}</span>
        <h3 className="agent-name">{name}</h3>
      </div>
      <div className="agent-rate">{rate != null ? `${rate.toFixed(1)}%` : '—'}</div>
      <div className="rate-label">Acceptance Rate</div>
      <div className="progress-track">
        <div className="progress-fill" style={{ width: `${rate ?? 0}%`, background: barColor }} />
      </div>
    </div>
  )
}

function StatusBadge({ status, label }) {
  const color = status === 'UP' ? '#22c55e' : status === 'DOWN' ? '#ef4444' : '#94a3b8'
  return (
    <div className="status-badge">
      <span className="status-dot" style={{ background: color }} />
      <span className="status-label">{label}</span>
      <span className="status-value">{status}</span>
    </div>
  )
}

export default function App() {
  const [metrics, setMetrics] = useState(null)
  const [health, setHealth] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [lastUpdated, setLastUpdated] = useState(null)

  const [chatReviewId, setChatReviewId] = useState('')
  const [chatMessage, setChatMessage] = useState('')
  const [chatHistory, setChatHistory] = useState([])
  const [chatLoading, setChatLoading] = useState(false)

  const handleSendChatMessage = async () => {
    if (!chatReviewId.trim() || !chatMessage.trim()) return

    const userMsg = chatMessage
    setChatMessage('')
    setChatHistory(prev => [...prev, { sender: 'user', text: userMsg }])
    setChatLoading(true)

    try {
      const response = await fetch(`${API_BASE}/reviews/${chatReviewId.trim()}/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ message: userMsg }),
      })

      if (!response.ok) {
        throw new Error(`Server returned status ${response.status}`)
      }

      const data = await response.json()
      setChatHistory(prev => [...prev, { sender: 'ai', text: data.response }])
    } catch (err) {
      setChatHistory(prev => [
        ...prev,
        { sender: 'ai', text: `Error: ${err.message || 'Could not reach review chat endpoint.'}` },
      ])
    } finally {
      setChatLoading(false)
    }
  }

  const agentIcons = {
    SECURITY: '🛡️',
    PERFORMANCE: '⚡',
    TESTING: '🧪',
    ARCHITECTURE: '🏗️'
  }

  const fetchData = useCallback(async () => {
    try {
      setError(null)

      const [metricsRes, healthRes] = await Promise.allSettled([
        fetch(`${API_BASE}/feedback/metrics`),
        fetch(`${API_BASE}/actuator/health`)
      ])

      if (metricsRes.status === 'fulfilled' && metricsRes.value.ok) {
        setMetrics(await metricsRes.value.json())
      }

      if (healthRes.status === 'fulfilled' && healthRes.value.ok) {
        setHealth(await healthRes.value.json())
      }

      setLastUpdated(new Date())
    } catch (err) {
      setError('Unable to connect to CodeGuardian API. Ensure the backend is running.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData()
    const interval = setInterval(fetchData, 30000)
    return () => clearInterval(interval)
  }, [fetchData])

  const agents = ['SECURITY', 'PERFORMANCE', 'TESTING', 'ARCHITECTURE']

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div className="header-content">
          <div className="logo-section">
            <span className="logo-icon">🤖</span>
            <div>
              <h1>CodeGuardian AI</h1>
              <p className="subtitle">Autonomous Pull Request Review Dashboard</p>
            </div>
          </div>
          <div className="header-actions">
            {lastUpdated && (
              <span className="last-updated">
                Last updated: {lastUpdated.toLocaleTimeString()}
              </span>
            )}
            <button className="refresh-btn" onClick={fetchData} disabled={loading}>
              {loading ? '⟳ Refreshing...' : '⟳ Refresh'}
            </button>
          </div>
        </div>
      </header>

      <main className="dashboard-main">
        {error && (
          <div className="error-banner">
            <span>⚠️</span> {error}
          </div>
        )}

        {/* System Health Section */}
        <section className="section">
          <h2 className="section-title">System Health</h2>
          <div className="health-grid">
            <StatusBadge
              status={health?.status || (loading ? '...' : 'UNKNOWN')}
              label="API Server"
            />
            {health?.components && Object.entries(health.components).map(([key, val]) => (
              <StatusBadge key={key} status={val.status || 'UNKNOWN'} label={key} />
            ))}
          </div>
        </section>

        {/* Agent Metrics Section */}
        <section className="section">
          <h2 className="section-title">Agent Acceptance Metrics</h2>
          <p className="section-desc">
            Track how often developers accept AI suggestions from each specialized review agent.
          </p>
          <div className="agents-grid">
            {agents.map(agent => (
              <AgentCard
                key={agent}
                name={agent}
                icon={agentIcons[agent] || '🔍'}
                rate={metrics?.[agent] ?? null}
              />
            ))}
          </div>
          {metrics && Object.keys(metrics).length === 0 && (
            <div className="empty-state">
              <span className="empty-icon">📊</span>
              <p>No feedback data yet. Submit feedback on review comments to populate metrics.</p>
            </div>
          )}
        </section>

        {/* Overview Stats */}
        <section className="section">
          <h2 className="section-title">Quick Stats</h2>
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-value">
                {metrics ? Object.keys(metrics).length : '—'}
              </div>
              <div className="stat-label">Active Agents</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">
                {metrics && Object.keys(metrics).length > 0
                  ? `${(Object.values(metrics).reduce((a, b) => a + b, 0) / Object.values(metrics).length).toFixed(1)}%`
                  : '—'}
              </div>
              <div className="stat-label">Avg Acceptance</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{health?.status === 'UP' ? '✓' : '✗'}</div>
              <div className="stat-label">System Status</div>
            </div>
          </div>
        </section>

        {/* Interactive Chat Section */}
        <section className="section">
          <h2 className="section-title">Interactive Review Chat</h2>
          <p className="section-desc">
            Enter a review UUID to start discussing suggestions and architectural improvements with Gemini.
          </p>
          <div className="chat-section">
            <div className="chat-input-row">
              <input
                type="text"
                className="chat-text-input"
                placeholder="Enter Review UUID (e.g. 550e8400-e29b-41d4-a716-446655440000)"
                value={chatReviewId}
                onChange={e => setChatReviewId(e.target.value)}
              />
            </div>
            <div className="chat-input-row">
              <input
                type="text"
                className="chat-text-input"
                placeholder="Ask Gemini about the review recommendations..."
                value={chatMessage}
                onChange={e => setChatMessage(e.target.value)}
                onKeyDown={e => {
                  if (e.key === 'Enter' && !chatLoading) {
                    handleSendChatMessage()
                  }
                }}
              />
              <button
                className="chat-btn"
                disabled={chatLoading || !chatReviewId.trim() || !chatMessage.trim()}
                onClick={handleSendChatMessage}
              >
                {chatLoading ? 'Sending...' : 'Send'}
              </button>
            </div>

            <div className="chat-messages">
              {chatHistory.length === 0 ? (
                <div className="chat-placeholder">
                  No chat messages yet. Enter a Review UUID and ask a question above to begin.
                </div>
              ) : (
                chatHistory.map((msg, index) => (
                  <div
                    key={index}
                    className={`chat-message ${msg.sender === 'user' ? 'message-user' : 'message-ai'}`}
                  >
                    <strong>{msg.sender === 'user' ? 'You' : 'Gemini'}:</strong> {msg.text}
                  </div>
                ))
              )}
            </div>
          </div>
        </section>
      </main>

      <footer className="dashboard-footer">
        <p>CodeGuardian AI © 2026 — Autonomous Pull Request Reviewer</p>
      </footer>
    </div>
  )
}
