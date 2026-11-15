import { useEffect, useRef, useState } from 'react';
import { facebookApi } from '../lib/apiClient';

const POLL_INTERVAL_MS = 5000;

export function useExtractionJobFeed(workspaceId, token, enabled) {
  const [jobs, setJobs] = useState([]);
  const [hasMore, setHasMore] = useState(false);
  const [activeCount, setActiveCount] = useState(0);
  const [finishedEvent, setFinishedEvent] = useState(null);
  const refreshRef = useRef(() => Promise.resolve());

  useEffect(() => {
    setFinishedEvent(null);
    setJobs([]);
    setHasMore(false);
    setActiveCount(0);
    if (!enabled) {
      refreshRef.current = () => Promise.resolve();
      return undefined;
    }
    let active = true;
    let knownStatuses = null;
    let loading = false;
    const feedStartedAt = Date.now();
    const refresh = async () => {
      if (loading) return;
      loading = true;
      try {
        const page = await facebookApi.extractionJobs(workspaceId, 0, token);
        if (!active) return;
        if (knownStatuses) {
          const newlyFinished = page.items.filter((job) => {
            const previous = knownStatuses.get(job.id);
            return ['COMPLETED', 'FAILED'].includes(job.status) &&
              (['QUEUED', 'RUNNING'].includes(previous) || (!previous && new Date(job.createdAt).getTime() >= feedStartedAt));
          });
          if (newlyFinished.length) setFinishedEvent({ jobs: newlyFinished, at: Date.now() });
        }
        knownStatuses = new Map(page.items.map((job) => [job.id, job.status]));
        setJobs(page.items);
        setHasMore(page.hasMore);
        setActiveCount(page.activeCount);
      } catch {
        // A transient polling error must not interrupt work elsewhere in the workspace.
      } finally {
        loading = false;
      }
    };
    refreshRef.current = refresh;
    refresh();
    const timer = window.setInterval(refresh, POLL_INTERVAL_MS);
    return () => { active = false; window.clearInterval(timer); };
  }, [workspaceId, token, enabled]);

  return { jobs, hasMore, activeCount, finishedEvent, dismissFinished: () => setFinishedEvent(null), refresh: () => refreshRef.current() };
}
