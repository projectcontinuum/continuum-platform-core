import { useMemo } from 'react';
import { JsonForms } from '@jsonforms/react';
import type { UISchemaElement, JsonSchema } from '@jsonforms/core';
import {
  materialRenderers,
  materialCells,
} from '@jsonforms/material-renderers';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { useTheme } from '../hooks/useTheme';
import { KeyValueEditor } from './KeyValueEditor';

interface DynamicFieldRendererProps {
  type: string;
  schema: Record<string, unknown>;
  uiSchema: Record<string, unknown>;
  data: Record<string, string>;
  onChange: (data: Record<string, string>) => void;
}

/**
 * Determine if this type/schema combination should fall back to the
 * KeyValueEditor instead of JSON Forms.
 *
 * Falls back when:
 *  - The type is "generic" (case-insensitive)
 *  - The schema uses additionalProperties (map/dictionary pattern)
 *  - The schema has no typed properties JSON Forms can render
 */
function shouldUseKeyValueEditor(type: string, schema: Record<string, unknown>): boolean {
  if (type.toLowerCase() === 'generic') return true;

  // Top-level additionalProperties → free-form map
  if (schema.additionalProperties) return true;

  if (schema.type !== 'object') return true;

  const props = schema.properties as Record<string, Record<string, unknown>> | undefined;
  if (!props || Object.keys(props).length === 0) return true;

  // If the only property itself is an object with additionalProperties,
  // that's a nested map pattern (like the current GENERIC schema)
  const keys = Object.keys(props);
  if (keys.length === 1) {
    const single = props[keys[0]];
    if (single?.type === 'object' && single?.additionalProperties) return true;
  }

  // Check there's at least one simple renderable property
  return !Object.values(props).some((prop) => {
    const t = prop.type;
    return t === 'string' || t === 'number' || t === 'integer' || t === 'boolean';
  });
}

/**
 * Validate that a uiSchema is in JSON Forms format.
 * JSON Forms expects objects with a "type" key (e.g., "VerticalLayout", "Control").
 */
function isJsonFormsUiSchema(uiSchema: Record<string, unknown>): boolean {
  return typeof uiSchema.type === 'string' && uiSchema.type !== '';
}

export function DynamicFieldRenderer({ type, schema, uiSchema, data, onChange }: DynamicFieldRendererProps) {
  const { isDark } = useTheme();

  const muiTheme = useMemo(() => createTheme({
    palette: {
      mode: isDark ? 'dark' : 'light',
      primary: {
        main: isDark ? '#C4A8FF' : '#705697',
      },
      background: {
        default: isDark ? '#292D3E' : '#F5F5F5',
        paper: isDark ? '#363C50' : '#FFFFFF',
      },
      text: {
        primary: isDark ? '#EEE7E7' : '#33384D',
        secondary: isDark ? '#A598B8' : '#6C6C6C',
      },
    },
    typography: {
      fontFamily: 'Inter, system-ui, sans-serif',
      fontSize: 14,
    },
    components: {
      MuiOutlinedInput: {
        styleOverrides: {
          root: { borderRadius: 8 },
        },
      },
      MuiFormLabel: {
        styleOverrides: {
          root: { fontSize: '0.875rem' },
        },
      },
    },
  }), [isDark]);

  // Fall back to KeyValueEditor for generic / map-style schemas
  if (shouldUseKeyValueEditor(type, schema)) {
    return <KeyValueEditor data={data} onChange={onChange} />;
  }

  // Only pass uiSchema to JSON Forms if it's in JSON Forms format
  const validUiSchema = isJsonFormsUiSchema(uiSchema)
    ? (uiSchema as unknown as UISchemaElement)
    : undefined;

  return (
    <ThemeProvider theme={muiTheme}>
      <CssBaseline enableColorScheme />
      <div className="json-forms-container">
        <JsonForms
          schema={schema as JsonSchema}
          uischema={validUiSchema}
          data={data}
          renderers={materialRenderers}
          cells={materialCells}
          onChange={({ data: newData }) => {
            if (newData) {
              const stringData: Record<string, string> = {};
              for (const [key, value] of Object.entries(newData as Record<string, unknown>)) {
                stringData[key] = value != null ? String(value) : '';
              }
              onChange(stringData);
            }
          }}
        />
      </div>
    </ThemeProvider>
  );
}
