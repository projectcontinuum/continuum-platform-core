import { useState } from 'react';
import { Modal } from './Modal';
import { Button } from './Button';
import { DEFAULT_RESOURCES, DEFAULT_IMAGE } from '../types/api';
import type { WorkbenchCreateRequest, ResourceSpec } from '../types/api';

interface CreateWorkbenchModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (request: WorkbenchCreateRequest) => Promise<void>;
}

export function CreateWorkbenchModal({ isOpen, onClose, onCreate }: CreateWorkbenchModalProps) {
  const [instanceName, setInstanceName] = useState('');
  const [image, setImage] = useState(DEFAULT_IMAGE);
  const [resources, setResources] = useState<ResourceSpec>(DEFAULT_RESOURCES);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!instanceName.trim()) {
      setError('Instance name is required');
      return;
    }

    // Validate instance name format (lowercase, alphanumeric, hyphens)
    if (!/^[a-z0-9][a-z0-9-]*[a-z0-9]$|^[a-z0-9]$/.test(instanceName)) {
      setError('Instance name must be lowercase, start and end with alphanumeric characters, and can contain hyphens');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await onCreate({
        instanceName: instanceName.trim(),
        image,
        resources,
      });

      // Reset form and close
      setInstanceName('');
      setImage(DEFAULT_IMAGE);
      setResources(DEFAULT_RESOURCES);
      setShowAdvanced(false);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create workbench');
    } finally {
      setLoading(false);
    }
  };

  const updateResource = (key: keyof ResourceSpec, value: string) => {
    setResources(prev => ({ ...prev, [key]: value }));
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Create New Workbench" size="lg">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="rounded-lg bg-red-100 p-3 text-sm text-red-800 dark:bg-red-900/30 dark:text-red-400">
            {error}
          </div>
        )}

        {/* Instance Name */}
        <div>
          <label htmlFor="instanceName" className="block text-sm font-medium text-fg">
            Instance Name <span className="text-red-500">*</span>
          </label>
          <input
            id="instanceName"
            type="text"
            value={instanceName}
            onChange={(e) => setInstanceName(e.target.value.toLowerCase())}
            placeholder="my-workbench"
            className="mt-1 w-full rounded-lg border border-divider bg-base px-3 py-2 text-fg placeholder:text-fg-muted/50 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
            required
          />
          <p className="mt-1 text-xs text-fg-muted">
            Lowercase letters, numbers, and hyphens only
          </p>
        </div>

        {/* Image */}
        <div>
          <label htmlFor="image" className="block text-sm font-medium text-fg">
            Container Image
          </label>
          <input
            id="image"
            type="text"
            value={image}
            onChange={(e) => setImage(e.target.value)}
            className="mt-1 w-full rounded-lg border border-divider bg-base px-3 py-2 text-fg placeholder:text-fg-muted/50 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
          />
        </div>

        {/* Advanced Options Toggle */}
        <button
          type="button"
          onClick={() => setShowAdvanced(!showAdvanced)}
          className="flex items-center gap-2 text-sm text-accent hover:text-highlight"
        >
          <svg
            className={`h-4 w-4 transition-transform ${showAdvanced ? 'rotate-90' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
          </svg>
          Advanced Resource Configuration
        </button>

        {/* Advanced Options */}
        {showAdvanced && (
          <div className="space-y-4 rounded-lg border border-divider bg-surface/50 p-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="cpuRequest" className="block text-sm font-medium text-fg">
                  CPU Request
                </label>
                <input
                  id="cpuRequest"
                  type="text"
                  value={resources.cpuRequest}
                  onChange={(e) => updateResource('cpuRequest', e.target.value)}
                  className="mt-1 w-full rounded-lg border border-divider bg-base px-3 py-2 text-fg focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
                />
              </div>
              <div>
                <label htmlFor="cpuLimit" className="block text-sm font-medium text-fg">
                  CPU Limit
                </label>
                <input
                  id="cpuLimit"
                  type="text"
                  value={resources.cpuLimit}
                  onChange={(e) => updateResource('cpuLimit', e.target.value)}
                  className="mt-1 w-full rounded-lg border border-divider bg-base px-3 py-2 text-fg focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
                />
              </div>
              <div>
                <label htmlFor="memoryRequest" className="block text-sm font-medium text-fg">
                  Memory Request
                </label>
                <input
                  id="memoryRequest"
                  type="text"
                  value={resources.memoryRequest}
                  onChange={(e) => updateResource('memoryRequest', e.target.value)}
                  className="mt-1 w-full rounded-lg border border-divider bg-base px-3 py-2 text-fg focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
                />
              </div>
              <div>
                <label htmlFor="memoryLimit" className="block text-sm font-medium text-fg">
                  Memory Limit
                </label>
                <input
                  id="memoryLimit"
                  type="text"
                  value={resources.memoryLimit}
                  onChange={(e) => updateResource('memoryLimit', e.target.value)}
                  className="mt-1 w-full rounded-lg border border-divider bg-base px-3 py-2 text-fg focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
                />
              </div>
              <div>
                <label htmlFor="storageSize" className="block text-sm font-medium text-fg">
                  Storage Size
                </label>
                <input
                  id="storageSize"
                  type="text"
                  value={resources.storageSize}
                  onChange={(e) => updateResource('storageSize', e.target.value)}
                  className="mt-1 w-full rounded-lg border border-divider bg-base px-3 py-2 text-fg focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
                />
              </div>
              <div>
                <label htmlFor="storageClassName" className="block text-sm font-medium text-fg">
                  Storage Class (optional)
                </label>
                <input
                  id="storageClassName"
                  type="text"
                  value={resources.storageClassName || ''}
                  onChange={(e) => updateResource('storageClassName', e.target.value || null as unknown as string)}
                  placeholder="default"
                  className="mt-1 w-full rounded-lg border border-divider bg-base px-3 py-2 text-fg placeholder:text-fg-muted/50 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
                />
              </div>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="flex justify-end gap-3 pt-4">
          <Button type="button" variant="secondary" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button type="submit" loading={loading}>
            Create Workbench
          </Button>
        </div>
      </form>
    </Modal>
  );
}

