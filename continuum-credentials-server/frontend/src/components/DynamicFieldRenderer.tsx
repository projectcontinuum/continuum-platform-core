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
 * Check if a schema has simple string/number/boolean properties that
 * JSON Forms material renderers can handle directly (i.e., flat fields,
 * not nested objects with additionalProperties).
 */
function hasRenderableProperties(schema: Record<string, unknown>): boolean {
  if (schema.type !== 'object') return false;
  const props = schema.properties as Record<string, Record<string, unknown>> | undefined;
  if (!props || Object.keys(props).length === 0) return false;

  // Check that at least one property is a simple type (string, number, boolean, integer)
  // and not a nested object with additionalProperties (which is a map/dictionary pattern)
  return Object.values(props).some((prop) => {
    const propType = prop.type;
    return propType === 'string' || propType === 'number' || propType === 'integer' || propType === 'boolean';
  });
}

/**
 * Check if a uiSchema is a valid JSON Forms UISchemaElement.
 * JSON Forms expects { "type": "...", "elements": [...] } or similar.
 */
function isValidJsonFormsUiSchema(uiSchema: Record<string, unknown>): boolean {
  return typeof uiSchema.type === 'string' && 'type' in uiSchema;
}

export function DynamicFieldRenderer({ type, schema, uiSchema, data, onChange }: DynamicFieldRendererProps) {
  const { isDark } = useTheme();

  // Create a MUI theme that adapts to Continuum's light/dark mode
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
          root: {
            borderRadius: 8,
          },
        },
      },
      MuiFormLabel: {
        styleOverrides: {
          root: {
            fontSize: '0.875rem',
          },
        },
      },
    },
  }), [isDark]);

  // Determine if this type should use the KeyValueEditor fallback:
  // - "generic" type (case-insensitive)
  // - Schema has no renderable simple properties (e.g., only has additionalProperties maps)
  // - Schema is empty or missing
  const isGenericType = type.toLowerCase() === 'generic';
  const renderable = hasRenderableProperties(schema);

  if (isGenericType || !renderable) {
    return <KeyValueEditor data={data} onChange={onChange} />;
  }

  // Only pass uiSchema if it's a valid JSON Forms UISchemaElement
  const validUiSchema = isValidJsonFormsUiSchema(uiSchema)
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
              // Ensure all values are strings
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
