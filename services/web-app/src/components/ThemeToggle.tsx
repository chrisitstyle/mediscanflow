"use client";

import { useSyncExternalStore } from "react";
import { Monitor, Moon, Sun } from "lucide-react";
import { useTheme } from "next-themes";

import { Button } from "@/components/ui/button";

const THEMES = ["system", "light", "dark"] as const;

type ThemeName = (typeof THEMES)[number];

const subscribe = () => () => {};
const getClientSnapshot = () => true;
const getServerSnapshot = () => false;

function isThemeName(theme: string | undefined): theme is ThemeName {
  return THEMES.includes(theme as ThemeName);
}

export function ThemeToggle() {
  const { theme, resolvedTheme, setTheme } = useTheme();

  const mounted = useSyncExternalStore(
    subscribe,
    getClientSnapshot,
    getServerSnapshot,
  );

  const currentTheme: ThemeName =
    mounted && isThemeName(theme) ? theme : "system";

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
      <span className="sr-only">
        Theme: {currentTheme}. Switch to {nextTheme}.
      </span>
    </Button>
  );
}
