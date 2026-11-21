import { useEffect, useState } from 'react';
import { askAi, standupInput, weeklyInput } from '../ai.js';
import { CloseIcon, CopyIcon, RefreshIcon, SparkleIcon } from './icons.jsx';

/** Entry card in the insights column. */
export function AiCard({ onOpen }) {
  return (
    <section className="icard ai-card">
      <div className="icard-head"><h2><SparkleIcon width={17} height={17} /> Ticked AI</h2></div>
      <p className="icard-sub">Turn what you've finished into a summary or a standup.</p>
      <div className="ai-actions">
        <button className="btn soft small" onClick={() => onOpen('weekly')}>Weekly review</button>
        <button className="btn soft small" onClick={() => onOpen('standup')}>Standup</button>
      </div>
    </section>
  );
}

/** Tiny Markdown renderer for the AI's answers: ## headings, - bullets, **bold**. */
function Rich({ text }) {
  const inline = s => s.split(/(\*\*[^*]+\*\*)/g).map((part, i) =>
    part.startsWith('**') && part.endsWith('**') ? <strong key={i}>{part.slice(2, -2)}</strong> : part);
  const blocks = [];
  let list = null;
  text.split('\n').forEach((raw, i) => {
    const line = raw.trim();
    const bullet = line.match(/^[-•*]\s+(.*)/);
    if (bullet) {
      if (!list) blocks.push(<ul key={`u${i}`}>{(list = [])}</ul>);
      list.push(<li key={i}>{inline(bullet[1])}</li>);
      return;
    }
    list = null;
    if (!line) return;
    const h = line.match(/^#{1,4}\s+(.*)/);
    blocks.push(h ? <h3 key={i}>{inline(h[1])}</h3> : <p key={i}>{inline(line)}</p>);
  });
  return <div className="rich">{blocks}</div>;
}

/** Weekly review / standup generator. */
export function AiModal({ kind: initialKind, tasks, onClose }) {
  const [kind, setKind] = useState(initialKind);
  const [plan, setPlan] = useState('');
  const [blockers, setBlockers] = useState('');
  const [result, setResult] = useState({ weekly: '', standup: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  const input = kind === 'weekly' ? weeklyInput(tasks) : standupInput(tasks);
  const text = result[kind];

  async function generate() {
    setBusy(true);
    setError('');
    setCopied(false);
    try {
      const out = await askAi(kind, kind === 'standup' ? { ...input, plan, blockers } : input);
      setResult(r => ({ ...r, [kind]: out }));
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  // The weekly review needs no input, so generate it straight away.
  useEffect(() => { if (kind === 'weekly' && !result.weekly && !busy) generate(); }, [kind]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const onKey = e => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  async function copy() {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    } catch {
      setError('Copy failed. Select the text and copy it manually.');
    }
  }

  return (
    <div className="overlay" onMouseDown={e => e.target === e.currentTarget && onClose()}>
      <div className="modal ai-modal" role="dialog" aria-modal="true" aria-labelledby="ai-title">
        <div className="modal-head">
          <h2 id="ai-title"><SparkleIcon width={18} height={18} /> Ticked AI</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close"><CloseIcon /></button>
        </div>

        <div className="tabs" role="tablist">
          {[['weekly', 'Weekly review'], ['standup', 'Standup']].map(([k, label]) => (
            <button key={k} role="tab" aria-selected={kind === k} className={`tab ${kind === k ? 'active' : ''}`}
              onClick={() => { setKind(k); setError(''); setCopied(false); }}>{label}</button>
          ))}
        </div>

        <p className="ai-scope">
          Based on {input.count} task{input.count === 1 ? '' : 's'} from {input.period}.
        </p>

        {kind === 'standup' && !text && (
          <>
            <label className="field">
              <span>Plans for today <em>optional</em></span>
              <textarea rows={2} value={plan} onChange={e => setPlan(e.target.value)} maxLength={1000}
                placeholder="e.g. Finish the sync design review, pair with Sam" />
            </label>
            <label className="field">
              <span>Blockers <em>optional</em></span>
              <input value={blockers} onChange={e => setBlockers(e.target.value)} maxLength={1000} placeholder="e.g. Waiting on API access" />
            </label>
          </>
        )}

        {busy && (
          <div className="ai-loading" aria-live="polite">
            <span className="ai-dot" /><span className="ai-dot" /><span className="ai-dot" />
            {kind === 'weekly' ? 'Reviewing your week…' : 'Writing your standup…'}
          </div>
        )}
        {error && <div className="alert">{error}</div>}
        {!busy && text && (kind === 'weekly' ? <Rich text={text} /> : (
          // Shown with Slack's *bold* lines rendered; Copy keeps the raw Slack text.
          <div className="standup">
            {text.split('\n').map((line, i) => {
              const h = line.trim().match(/^\*([^*]+)\*$/);
              return h ? <strong key={i}>{h[1]}</strong> : <div key={i}>{line || '\u00a0'}</div>;
            })}
          </div>
        ))}

        <div className="modal-foot">
          {text && !busy && (
            <button className="btn ghost push" onClick={generate}><RefreshIcon width={16} height={16} /> Regenerate</button>
          )}
          {text && !busy && (
            <button className="btn primary" onClick={copy}><CopyIcon width={16} height={16} /> {copied ? 'Copied' : 'Copy'}</button>
          )}
          {kind === 'standup' && !text && !busy && (
            <button className="btn primary" onClick={generate}><SparkleIcon width={16} height={16} /> Generate standup</button>
          )}
          {kind === 'weekly' && !text && !busy && error && (
            <button className="btn primary" onClick={generate}>Try again</button>
          )}
        </div>
        <p className="ai-note">AI can make mistakes. Your task list is sent to Groq only to write this text.</p>
      </div>
    </div>
  );
}
