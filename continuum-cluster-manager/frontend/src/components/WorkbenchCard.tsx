import { useState } from 'react';
import { motion } from 'framer-motion';
import type { WorkbenchResponse } from '../types/api';
import { StatusBadge } from './StatusBadge';
import { Button } from './Button';
import { Modal } from './Modal';

interface WorkbenchCardProps {
  workbench: WorkbenchResponse;
  onSuspend: (instanceName: string) => Promise<void>;
  onResume: (instanceName: string) => Promise<void>;
  onDelete: (instanceName: string) => Promise<void>;
  onOpen: (workbench: WorkbenchResponse) => void;
}

export function WorkbenchCard({ workbench, onSuspend, onResume, onDelete, onOpen }: WorkbenchCardProps) {
  const [loading, setLoading] = useState<string | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const handleAction = async (action: string, handler: () => Promise<void>) => {
    setLoading(action);
    try {
      await handler();
    } finally {
      setLoading(null);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  const isRunning = workbench.status === 'RUNNING';
  const isSuspended = workbench.status === 'SUSPENDED';
  const isPending = workbench.status === 'PENDING';
  const canSuspend = isRunning;
  const canResume = isSuspended;
  const canDelete = !isPending;
  const canOpen = isRunning;

  return (
    <>
      <motion.article
        layout
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -20 }}
        whileHover={{ y: -4 }}
        className="group relative rounded-xl border border-divider bg-card p-5 transition-shadow hover:glow-accent"
      >
        {/* Header */}
        <div className="mb-4 flex items-start justify-between">
          <div>
            <h3 className="text-lg font-semibold text-fg">{workbench.instanceName}</h3>
            <p className="mt-1 text-xs text-fg-muted">
              ID: {workbench.instanceId.slice(0, 8)}...
            </p>
          </div>
          <StatusBadge status={workbench.status} />
        </div>

        {/* Details */}
        <div className="mb-4 space-y-2 text-sm">
          <div className="flex items-center justify-between">
            <span className="text-fg-muted">Image:</span>
            <span className="truncate max-w-[200px] text-fg" title={workbench.image}>
              {workbench.image.split('/').pop()}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-fg-muted">Resources:</span>
            <span className="text-fg">
              {workbench.resources.cpuRequest} CPU, {workbench.resources.memoryRequest} RAM
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-fg-muted">Storage:</span>
            <span className="text-fg">{workbench.resources.storageSize}</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-fg-muted">Created:</span>
            <span className="text-fg">{formatDate(workbench.createdAt)}</span>
          </div>
        </div>

        {/* Service Endpoint */}
        {workbench.serviceEndpoint && isRunning && (
          <div className="mb-4 rounded-lg bg-surface/50 p-2">
            <p className="text-xs text-fg-muted">Service Endpoint:</p>
            <code className="text-xs text-accent break-all">{workbench.serviceEndpoint}</code>
          </div>
        )}

        {/* Actions */}
        <div className="flex flex-wrap gap-2">
          {canOpen && (
            <Button
              size="sm"
              onClick={() => onOpen(workbench)}
              disabled={loading !== null}
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
              Open
            </Button>
          )}

          {canSuspend && (
            <Button
              size="sm"
              variant="secondary"
              onClick={() => handleAction('suspend', () => onSuspend(workbench.instanceName))}
              loading={loading === 'suspend'}
              disabled={loading !== null && loading !== 'suspend'}
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Suspend
            </Button>
          )}

          {canResume && (
            <Button
              size="sm"
              onClick={() => handleAction('resume', () => onResume(workbench.instanceName))}
              loading={loading === 'resume'}
              disabled={loading !== null && loading !== 'resume'}
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Resume
            </Button>
          )}

          {canDelete && (
            <Button
              size="sm"
              variant="danger"
              onClick={() => setShowDeleteConfirm(true)}
              disabled={loading !== null}
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              Delete
            </Button>
          )}
        </div>
      </motion.article>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        title="Delete Workbench"
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-fg-muted">
            Are you sure you want to delete <span className="font-semibold text-fg">{workbench.instanceName}</span>?
            This action cannot be undone and all data will be permanently lost.
          </p>
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setShowDeleteConfirm(false)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              loading={loading === 'delete'}
              onClick={async () => {
                await handleAction('delete', () => onDelete(workbench.instanceName));
                setShowDeleteConfirm(false);
              }}
            >
              Delete
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}

