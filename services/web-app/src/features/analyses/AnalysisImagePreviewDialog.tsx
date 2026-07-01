"use client";

import { ExternalLink, Maximize2 } from "lucide-react";

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

type AnalysisImagePreviewDialogProps = {
  title: string;
  imageUrl: string;
  alt: string;
};

export function AnalysisImagePreviewDialog({
  title,
  imageUrl,
  alt,
}: AnalysisImagePreviewDialogProps) {
  return (
    <Dialog>
      <DialogTrigger asChild>
        <button
          type="button"
          className="group relative block w-full overflow-hidden rounded-lg border bg-muted text-left transition hover:border-primary/50"
          aria-label={`Open ${title} preview`}
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={imageUrl}
            alt={alt}
            className="aspect-video w-full object-contain transition group-hover:scale-[1.01]"
          />

          <span className="absolute right-3 top-3 inline-flex items-center gap-2 rounded-full border bg-background/90 px-3 py-1 text-xs font-medium text-foreground opacity-0 shadow-sm backdrop-blur transition group-hover:opacity-100">
            <Maximize2 className="size-3.5" />
            Preview
          </span>
        </button>
      </DialogTrigger>

      <DialogContent className="max-h-[92vh] overflow-hidden sm:max-w-6xl">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>
            Large preview of the selected medical scan image.
          </DialogDescription>
        </DialogHeader>

        <div className="flex max-h-[72vh] items-center justify-center overflow-auto rounded-lg border bg-muted/40 p-4">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={imageUrl}
            alt={alt}
            className="max-h-[68vh] w-auto max-w-full object-contain"
          />
        </div>

        <DialogFooter>
          <Button asChild variant="outline">
            <a href={imageUrl} target="_blank" rel="noreferrer">
              <ExternalLink className="size-4" />
              Open image in new tab
            </a>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
