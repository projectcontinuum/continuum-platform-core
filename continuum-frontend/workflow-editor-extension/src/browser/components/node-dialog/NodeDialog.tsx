import * as React from 'react';
import DialogTitle from '@mui/material/DialogTitle';
import Dialog from '@mui/material/Dialog';
import { Box, Button, DialogActions, DialogContent, IconButton, Typography, styled } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
// @ts-ignore-next-line
import { JsonForms } from '@jsonforms/react';
// @ts-ignore-next-line
import { materialCells, materialRenderers } from '@jsonforms/material-renderers';
import { JsonFormsCore, JsonSchema, UISchemaElement } from '@jsonforms/core';
import knimeNodeDialog from 'knime-core-ui/dist/NodeDialog';
import ContinuumUIExtensionService from "../../service/ContinuumUIExtentionService";
import mockForm from '../../service/mocks/advancedSettings.json'
import './NodeDialog.css';
// import "@fontsource/roboto/files/"

const StyledDialog = styled(Dialog)(({ theme }) => ({
    '& .MuiDialogContent-root': {
      padding: theme.spacing(2),
    },
    '& .MuiDialogActions-root': {
      padding: theme.spacing(1),
    },
}));

export interface NodeDialogProps {
    open: boolean;
    // selectedValue: string;
    onClose: (value: any) => void;
    onSave: (data: any) => void;
    initialData?: any;
    dataSchema?: JsonSchema;
    uiSchema?: UISchemaElement;
    readOnly?: boolean;
}

export default function NodeDialog({ onClose, onSave, readOnly=false, open, initialData, dataSchema, uiSchema }: NodeDialogProps) {
    // @ts-ignore-next-line
    const [data, setData] = React.useState(initialData);
    // @ts-ignore-next-line
    const [hasErrors, setHasErrors] = React.useState(false);

    const handleClose = React.useCallback((args: any) => {
        console.log("handleClose", args);
        onClose(data);
    }, [data]);

    const onSavePressed = React.useCallback((args: any) => {
        console.log("onSavePressed", args);
        onSave(data);
    }, [data]);
    // @ts-ignore-next-line
    const onDataChange = React.useCallback(({errors, data}: Pick<JsonFormsCore, "data" | "errors">) => {
        setData(data);
        errors && setHasErrors(errors.length > 0)
    }, [data]);

    const vueContainerRef = React.useRef<HTMLDivElement>(null);

    React.useEffect(() => {
        if (open) {
            const timer = setTimeout(() => {
                if (vueContainerRef.current) {
                    console.log("knimeNodeDialog Loaded...", knimeNodeDialog);
                    let continuumUIExtensionService = new ContinuumUIExtensionService(
                            "NodeDialog",
                            mockForm.result.data,
                            mockForm.result.schema,
                            mockForm.result.ui_schema
                    );
                    knimeNodeDialog(vueContainerRef.current, continuumUIExtensionService);
                } else {
                    console.error("Vue container ref is not set");
                }
            }, 0); // Delay execution to ensure DOM is rendered

            return () => clearTimeout(timer); // Cleanup timer
        }
    }, [vueContainerRef, open, knimeNodeDialog]);

    return (
        <StyledDialog 
            open={open} 
            onClose={handleClose} >
            <DialogTitle>Node Settings</DialogTitle>
            <IconButton
                aria-label="close"
                onClick={handleClose}
                sx={{
                    position: 'absolute',
                    right: 8,
                    top: 8,
                    color: (theme) => theme.palette.grey[500],
                }}>
                <CloseIcon />
            </IconButton>
            <DialogContent dividers>
                <Box sx={{
                    minWidth: "500px",
                    minHeight: "500px",
                    p: 5}}>
                    <div ref={vueContainerRef} />
                    {/*<JsonForms*/}
                    {/*    schema={dataSchema}*/}
                    {/*    uischema={uiSchema}*/}
                    {/*    data={data}*/}
                    {/*    renderers={materialRenderers}*/}
                    {/*    cells={materialCells}*/}
                    {/*    onChange={onDataChange}/>*/}
                </Box>
            </DialogContent>
            <DialogActions>
                <Button autoFocus onClick={handleClose}>
                    Cancel
                </Button>
                <Button 
                    autoFocus 
                    onClick={onSavePressed} 
                    disabled={hasErrors || readOnly}>
                    <Typography>Save changes</Typography>
                </Button>
            </DialogActions>
        </StyledDialog>
    );
}