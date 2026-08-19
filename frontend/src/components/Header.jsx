const CONNECTION_META = {
  checking: { label: 'Checking...', tone: 'checking' },
  connected: { label: 'Connected', tone: 'connected' },
  disconnected: { label: 'Disconnected', tone: 'disconnected' },
}

function Header({ connection }) {
  const meta = CONNECTION_META[connection] || CONNECTION_META.checking

  return (
    <header className="header">
      <div className="header-inner">
        <div className="header-brand">
          <span className="header-mark" aria-hidden="true">
            <svg width="26" height="26" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="6" fill="#0FA4AF" />
              <path
                d="M5 14h4l2-6 4 12 2-6h6"
                stroke="#FFFFFF"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </span>
          <div>
            <h1 className="header-title">API Rate Limiter</h1>
            <p className="header-subtitle">Redis-backed request protection</p>
          </div>
        </div>
        <div className="connection" role="status" aria-label={`Backend ${meta.label}`}>
          <span className={`connection-dot connection-dot--${meta.tone}`} aria-hidden="true" />
          <span className="connection-text">{meta.label}</span>
        </div>
      </div>
    </header>
  )
}

export default Header
