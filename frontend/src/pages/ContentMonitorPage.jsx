import { useState } from 'react';
import { Icon } from '../components/ui/Icon';
import { facebookApi, workspaceApi } from '../lib/apiClient';
import { useI18n } from '../i18n/I18nContext';

export function ContentMonitorPage({ results, history, onResultsChange, onHistoryChange, workspaceId, token, view, onUseForExtraction }) {
  const [keyword, setKeyword] = useState('');
  const [limit, setLimit] = useState(10);
  const [recentPosts, setRecentPosts] = useState(true);
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const { t } = useI18n();

  async function submit(event) {
    event.preventDefault();
    if (!keyword.trim() || loading) return;
    setLoading(true);
    setStatus(null);
    try {
      const data = await facebookApi.searchPosts({ keyword: keyword.trim(), limit: Number(limit), recentPosts });
      const snapshot = await workspaceApi.saveMonitor(workspaceId, { keyword: keyword.trim(), results: data }, token);
      onResultsChange(snapshot.results);
      onHistoryChange(snapshot.history);
      setStatus({ type: 'success', text: t('monitor.found', { count: data.length }) });
    } catch (error) {
      setStatus({ type: 'error', text: error.message });
    } finally {
      setLoading(false);
    }
  }

  async function copyLink(url) {
    try { await navigator.clipboard.writeText(url); } catch { setStatus({ type: 'error', text: t('monitor.copyError') }); }
  }

  async function toggleStar(post) {
    try {
      const updated = await workspaceApi.toggleMonitorStar(workspaceId, post.id, token);
      onResultsChange((current) => current.map((item) => item.id === post.id ? updated : item));
    } catch (error) { setStatus({ type: 'error', text: error.message }); }
  }

  async function clearResults() {
    if (!results.length || !window.confirm(t('monitor.confirmClear'))) return;
    await workspaceApi.clearMonitorResults(workspaceId, token);
    onResultsChange([]);
    setStatus({ type: 'success', text: t('monitor.cleared') });
  }

  const visiblePosts = view === 'following' ? results.filter((post) => post.starred) : results;

  return <section className="page-panel content-monitor-page"><div className="panel-head"><div><p className="eyebrow">{t('monitor.eyebrow')}</p><h2>{view === 'following' ? t('monitor.followingTitle') : t('monitor.searchTitle')}</h2><p className="panel-subtitle">{view === 'following' ? t('monitor.followingText') : t('monitor.searchText')}</p></div><span className="status-badge available"><Icon name="search" size={13} /> MVP</span></div>
    {view === 'search' && <>
    <form className="monitor-search-form" onSubmit={submit}><label>{t('monitor.keyword')}<div className="monitor-input"><Icon name="search" size={17} /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder={t('monitor.placeholder')} required /></div></label><label>{t('monitor.limit')}<select value={limit} onChange={(event) => setLimit(event.target.value)}>{[10,20,50].map((count) => <option key={count} value={count}>{t('monitor.posts', { count })}</option>)}</select></label><label className="recent-posts-toggle"><input type="checkbox" checked={recentPosts} onChange={(event) => setRecentPosts(event.target.checked)} /><span /><div><strong>{t('monitor.recent')}</strong><small>{t('monitor.recentHelp')}</small></div></label><button className="primary-button" disabled={loading}>{loading ? t('monitor.searching') : t('monitor.search')} <span>→</span></button></form>
    {status && <div className={`notice ${status.type}`}>{status.type === 'success' ? '✓' : '!'} {status.text}</div>}
    {history.length > 0 && <div className="monitor-history"><span>{t('monitor.recentSearches')}</span>{history.slice(0, 4).map((item) => <button type="button" key={item.id} onClick={() => setKeyword(item.keyword)}>{item.keyword} <b>{item.count}</b></button>)}</div>}</>}
    <div className="monitor-results-head">{view === 'search' && results.length > 0 && <><span>{t('monitor.savedCount', { count: results.length })}</span><button type="button" className="danger-button" onClick={clearResults}>{t('monitor.clear')}</button></>}</div><div className="monitor-results">{visiblePosts.map((post) => <article className="monitor-result" key={post.id ?? post.url}><div className="monitor-result-main"><a href={post.url} target="_blank" rel="noreferrer"><strong>{post.author || t('monitor.facebookPost')}</strong> ↗</a><p>{post.text || t('monitor.noContent')}</p></div><div className="monitor-result-actions"><button className={post.starred ? 'star-button starred' : 'star-button'} type="button" onClick={() => toggleStar(post)}>{post.starred ? `★ ${t('monitor.following')}` : `☆ ${t('monitor.follow')}`}</button>{post.starred && <button className="primary-button" type="button" onClick={() => onUseForExtraction(post.url)}>{t('monitor.useExtract')}</button>}<button className="ghost-button" type="button" onClick={() => copyLink(post.url)}>{t('common.copyLink')}</button></div></article>)}{!visiblePosts.length && !loading && <div className="approval-empty"><span><Icon name="search" size={28} /></span><h3>{view === 'following' ? t('monitor.noFollowing') : t('monitor.noResults')}</h3><p>{t('monitor.emptyText')}</p></div>}</div>
  </section>;
}
