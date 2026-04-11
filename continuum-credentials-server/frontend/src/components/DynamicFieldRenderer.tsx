import { useMemo } from 'react';
import { JsonForms } from '@jsonforms/react';
import type { UISchemaElement } from '@jsonforms/core';
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

  // For "generic" type with empty/permissive schema, use KeyValueEditor
  const isGenericType = type === 'generic';
  const hasNoProperties = !schema.properties || Object.keys(schema.properties as object).length === 0;

  if (isGenericType || hasNoProperties) {
    return <KeyValueEditor data={data} onChange={onChange} />;
  }

  return (
    <ThemeProvider theme={muiTheme}>
      <CssBaseline enableColorScheme />
      <div className="json-forms-container">
        <JsonForms
          schema={schema}
          uischema={Object.keys(uiSchema).length > 0 ? (uiSchema as unknown as UISchemaElement) : undefined}
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
