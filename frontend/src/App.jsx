import { useCallback, useEffect, useState } from 'react'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  })
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`)
  }
  return response.json()
}

function formatMoney(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
}

function Confidence({ value }) {
  return <span className="confidence">{Math.round(value * 100)}%</span>
}

function Suggestion({ suggestion, type, onDecision, busy }) {
  if (!suggestion) {
    return <span className="muted">No pending suggestion</span>
  }

  const isPricing = type === 'pricing'
  const acceptLabel = isPricing ? `Accept ${formatMoney(suggestion.recommendedPrice)}` : `Accept ${suggestion.recommendedQuantity} units`

  return (
    <div className="suggestion">
      <div className="suggestion-head">
        <span className="badge">{suggestion.triggerReason.replace('_', ' ')}</span>
        <Confidence value={suggestion.confidence} />
      </div>
      <strong>{isPricing ? formatMoney(suggestion.recommendedPrice) : `${suggestion.recommendedQuantity} units`}</strong>
      <p>{suggestion.reasoning}</p>
      <div className="actions">
        <button disabled={busy} onClick={() => onDecision(type, suggestion, 'accept')}>
          {busy ? 'Working...' : acceptLabel}
        </button>
        <button className="quiet" disabled={busy} onClick={() => onDecision(type, suggestion, 'reject')}>
          Reject
        </button>
      </div>
    </div>
  )
}

function App() {
  const [products, setProducts] = useState([])
  const [suggestions, setSuggestions] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState('')
  const [saleQuantities, setSaleQuantities] = useState({})

  const loadData = useCallback(async (showLoading = false) => {
    if (showLoading) setLoading(true)
    try {
      const productList = await request('/api/products')
      const pending = await Promise.all(productList.map(async (product) => {
        const [pricing, reorder] = await Promise.all([
          request(`/api/products/${product.id}/pricing-suggestions?status=PENDING`),
          request(`/api/products/${product.id}/reorder-suggestions?status=PENDING`),
        ])
        return [product.id, { pricing: pricing[0], reorder: reorder[0] }]
      }))
      setProducts(productList)
      setSuggestions(Object.fromEntries(pending))
      setError('')
    } catch (loadError) {
      setError(`Could not load StockPulse: ${loadError.message}`)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData(true)
    const interval = window.setInterval(() => loadData(), 2000)
    return () => window.clearInterval(interval)
  }, [loadData])

  async function decide(type, suggestion, decision) {
    const productId = suggestion.productId
    const key = `${type}-${suggestion.id}`
    setBusy(key)
    try {
      await request(`/api/products/${productId}/${type}-suggestions/${suggestion.id}/${decision}`, { method: 'POST' })
      await loadData()
    } catch (actionError) {
      setError(`Action failed: ${actionError.message}`)
    } finally {
      setBusy('')
    }
  }

  async function simulateSale(product) {
    const quantity = Number(saleQuantities[product.id] || 1)
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > product.stockLevel) {
      setError(`Sale quantity for ${product.name} must be between 1 and ${product.stockLevel}.`)
      return
    }
    setBusy(`sale-${product.id}`)
    try {
      await request(`/api/products/${product.id}`, {
        method: 'PUT',
        body: JSON.stringify({ stockLevel: product.stockLevel - quantity }),
      })
      await loadData()
    } catch (actionError) {
      setError(`Sale failed: ${actionError.message}`)
    } finally {
      setBusy('')
    }
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">STOCKPULSE / OPERATIONS</p>
          <h1>Inventory decisions, in one view.</h1>
          <p className="subhead">Monitor live stock signals and review pending commerce recommendations.</p>
        </div>
        <div className="sync-status"><span className="pulse" /> Polling every 2 seconds</div>
      </header>

      {error && <div className="error" role="alert">{error}<button onClick={() => setError('')}>Dismiss</button></div>}
      {loading ? <div className="state">Loading products and suggestions...</div> : (
        <section className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Product</th><th>Stock</th><th>Threshold</th><th>Price</th><th>Velocity</th><th>Status</th><th>Pricing suggestion</th><th>Reorder suggestion</th><th>Simulate sale</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => {
                const row = suggestions[product.id] || {}
                return (
                  <tr key={product.id}>
                    <td><strong>{product.name}</strong><span className="sku">{product.sku}</span></td>
                    <td className={product.stockLevel < product.reorderThreshold ? 'stock-low' : ''}>{product.stockLevel}</td>
                    <td>{product.reorderThreshold}</td>
                    <td>{formatMoney(product.currentPrice)}</td>
                    <td>{product.demandVelocity}/day</td>
                    <td><span className={`status status-${product.status.toLowerCase()}`}>{product.status.replaceAll('_', ' ')}</span></td>
                    <td><Suggestion suggestion={row.pricing} type="pricing" onDecision={decide} busy={busy === `pricing-${row.pricing?.id}`} /></td>
                    <td><Suggestion suggestion={row.reorder} type="reorder" onDecision={decide} busy={busy === `reorder-${row.reorder?.id}`} /></td>
                    <td>
                      <div className="sale-control">
                        <input aria-label={`Sale quantity for ${product.name}`} type="number" min="1" max={product.stockLevel} value={saleQuantities[product.id] || 1} onChange={(event) => setSaleQuantities({ ...saleQuantities, [product.id]: event.target.value })} />
                        <button disabled={busy === `sale-${product.id}`} onClick={() => simulateSale(product)}>{busy === `sale-${product.id}` ? '...' : 'Record sale'}</button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </section>
      )}
      {!loading && products.length === 0 && <div className="state">No products found.</div>}
    </main>
  )
}

export default App
