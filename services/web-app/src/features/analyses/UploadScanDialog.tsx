"use client";

import type React from "react";
import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { uploadScan } from "@/api/analysesApi";
import { ApiClientError } from "@/lib/apiClient";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { queryKeys } from "@/lib/queryKeys";

type UploadScanDialogProps = {
  patientId: string;
};

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
const ALLOWED_CONTENT_TYPES = ["image/jpeg", "image/png"];

function validateFile(file: File | null) {
  if (!file) {
    return "Scan file is required.";
  }

  if (file.size === 0) {
    return "File must not be empty.";
  }

  if (file.size > MAX_FILE_SIZE_BYTES) {
    return "File size must not exceed 10 MB.";
  }

  if (!ALLOWED_CONTENT_TYPES.includes(file.type)) {
    return "Unsupported file type. Please upload a JPG or PNG image.";
  }

  return null;
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export function UploadScanDialog({ patientId }: UploadScanDialogProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const [open, setOpen] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: uploadScan,
    onSuccess: async (analysis) => {
      toast.success("Scan uploaded", {
        description: "AI analysis was queued successfully.",
      });

      queryClient.setQueryData(
        queryKeys.analyses.detail(analysis.id),
        analysis,
      );

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.patients.analyses(patientId),
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.dashboard.summary(),
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.analyses.recent(),
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.analyses.list(),
        }),
      ]);

      setOpen(false);
      router.push(`/analyses/${analysis.id}`);
    },
    onError: (error) => {
      toast.error("Upload failed", {
        description:
          error instanceof ApiClientError
            ? error.message
            : "Unexpected error while uploading scan.",
      });
    },
  });

  const submitError =
    mutation.error instanceof ApiClientError
      ? mutation.error.message
      : mutation.isError
        ? "Could not upload scan. Please try again."
        : null;

  function resetForm() {
    setFile(null);
    setFileError(null);
    mutation.reset();

    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  function handleOpenChange(nextOpen: boolean) {
    setOpen(nextOpen);

    if (!nextOpen) {
      resetForm();
    }
  }

  function handleFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const selectedFile = event.target.files?.[0] ?? null;

    setFile(selectedFile);
    setFileError(validateFile(selectedFile));
    mutation.reset();
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const nextFileError = validateFile(file);
    setFileError(nextFileError);

    if (nextFileError || !file) {
      return;
    }

    mutation.mutate({
      patientId,
      file,
    });
  }

  function handleClearFile() {
    setFile(null);
    setFileError(null);
    mutation.reset();

    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button>Upload scan</Button>
      </DialogTrigger>

      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Upload scan</DialogTitle>
          <DialogDescription>
            Upload a JPG or PNG medical scan image. The scan will be queued for
            asynchronous AI analysis.
          </DialogDescription>
        </DialogHeader>

        {submitError && (
          <Alert variant="destructive">
            <AlertTitle>Could not upload scan</AlertTitle>
            <AlertDescription>{submitError}</AlertDescription>
          </Alert>
        )}

        <form onSubmit={handleSubmit} noValidate className="space-y-6">
          <FieldGroup>
            <Field data-invalid={!!fileError}>
              <FieldLabel htmlFor="scanFile">Scan image</FieldLabel>

              <Input
                ref={fileInputRef}
                id="scanFile"
                type="file"
                accept="image/jpeg,image/png"
                onChange={handleFileChange}
                disabled={mutation.isPending}
                aria-invalid={!!fileError}
              />

              <FieldDescription>
                Accepted formats: JPG, PNG. Maximum file size: 10 MB. Default
                model: yolo-brain-tumor-detector / yolov8n.
              </FieldDescription>

              {fileError && <FieldError>{fileError}</FieldError>}
            </Field>

            {file && (
              <div className="rounded-lg border bg-muted/40 p-4 text-sm">
                <div className="font-medium">{file.name}</div>
                <div className="mt-1 text-muted-foreground">
                  {file.type || "unknown type"} · {formatFileSize(file.size)}
                </div>

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="mt-4"
                  onClick={handleClearFile}
                  disabled={mutation.isPending}
                >
                  Remove file
                </Button>
              </div>
            )}
          </FieldGroup>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setOpen(false)}
              disabled={mutation.isPending}
            >
              Cancel
            </Button>

            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Uploading..." : "Upload and analyze"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
