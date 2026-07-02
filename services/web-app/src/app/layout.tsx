import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";

import { AppNavbar } from "@/components/AppNavbar";
import { ProtectedApp } from "@/components/ProtectedApp";
import { Providers } from "@/app/providers";
import { Toaster } from "@/components/ui/sonner";

import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "MediScanFlow",
  description: "Medical image analysis platform",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} bg-background`}
    >
      <body className="font-sans antialiased">
        <Providers>
          <ProtectedApp>
            <AppNavbar />
            {children}
          </ProtectedApp>
          <Toaster />
        </Providers>
      </body>
    </html>
  );
}
