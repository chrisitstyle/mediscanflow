"use client";

import type React from "react";
import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { uploadScan } from "@/api/analysesApi";
import { ApiClientError } from "@/lib/apiClient";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";

type UploadScanFormProps = {
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

export function UploadScanForm({ patientId }: UploadScanFormProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: uploadScan,
    onSuccess: async (analysis) => {
      await queryClient.invalidateQueries({
        queryKey: ["patients", patientId, "analyses"],
      });

      queryClient.setQueryData(["analyses", analysis.id], analysis);

      router.push(`/analyses/${analysis.id}`);
    },
  });

  const submitError =
    mutation.error instanceof ApiClientError
      ? mutation.error.message
      : mutation.isError
        ? "Could not upload scan. Please try again."
        : null;

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
    <Card id="upload-scan">
      <CardHeader>
        <Badge variant="secondary" className="w-fit">
          Upload
        </Badge>

        <CardTitle className="mt-4">Upload scan</CardTitle>

        <CardDescription>
          Upload a JPG or PNG medical scan image. The scan will be queued for
          asynchronous AI analysis.
        </CardDescription>
      </CardHeader>

      <CardContent>
        {submitError && (
          <Alert variant="destructive" className="mb-6">
            <AlertTitle>Could not upload scan</AlertTitle>
            <AlertDescription>{submitError}</AlertDescription>
          </Alert>
        )}

        <form onSubmit={handleSubmit} noValidate>
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

            <div className="flex justify-end">
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? "Uploading..." : "Upload and analyze"}
              </Button>
            </div>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  );
}
