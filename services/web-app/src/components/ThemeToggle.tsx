"use client";

import { useEffect, useState } from "react";
import { Monitor, Moon, Sun } from "lucide-react";
import { useTheme } from "next-themes";

import { Button } from "@/components/ui/button";

const THEMES = ["system", "light", "dark"] as const;

type ThemeName = (typeof THEMES)[number];

function isThemeName(theme: string | undefined): theme is ThemeName {
  return THEMES.includes(theme as ThemeName);
}

export function ThemeToggle() {
  const { theme, resolvedTheme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const currentTheme: ThemeName = isThemeName(theme) ? theme : "system";
  const nextTheme = THEMES[(THEMES.indexOf(currentTheme) + 1) % THEMES.length];

  const Icon =
    currentTheme === "system" ? Monitor : resolvedTheme === "dark" ? Moon : Sun;

  return (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      className="rounded-full"
      disabled={!mounted}
      onClick={() => setTheme(nextTheme)}
      title={`Theme: ${currentTheme}. Switch to ${nextTheme}.`}
      aria-label={`Theme: ${currentTheme}. Switch to ${nextTheme}.`}
    >
      <Icon className="size-4" aria-hidden="true" />
      <span className="sr-only">Toggle theme</span>
    </Button>
  );
}
