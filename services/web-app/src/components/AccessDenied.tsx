import Link from "next/link";
import { ShieldAlert } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

type AccessDeniedProps = {
  title?: string;
  description?: string;
  backHref?: string;
  backLabel?: string;
};

export function AccessDenied({
  title = "Access denied",
  description = "Your account does not have permission to perform this action.",
  backHref = "/",
  backLabel = "Back to dashboard",
}: AccessDeniedProps) {
  return (
    <main className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-6xl items-center justify-center px-6 py-8">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="mx-auto mb-2 flex size-12 items-center justify-center rounded-full bg-destructive/10">
            <ShieldAlert className="size-6 text-destructive" />
          </div>

          <CardTitle>{title}</CardTitle>

          <CardDescription>{description}</CardDescription>
        </CardHeader>

        <CardContent>
          <Button asChild className="w-full">
            <Link href={backHref}>{backLabel}</Link>
          </Button>
        </CardContent>
      </Card>
    </main>
  );
}
