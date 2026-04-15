"use client";

import { useRouter } from "next/navigation";
import { ChevronLeft } from "lucide-react";
import { Button } from "@/components/ui/button";

interface BackButtonProps {
  label?: string;
  href?: string;
}

export function BackButton({ label, href }: BackButtonProps) {
  const router = useRouter();
  const handleClick = () => {
    if (href) {
      router.push(href);
    } else {
      router.back();
    }
  };
  return (
    <Button
      variant="ghost"
      size="sm"
      className="gap-1 pl-0 text-muted-foreground hover:text-foreground"
      onClick={handleClick}
    >
      <ChevronLeft className="h-4 w-4" />
      {label ?? "뒤로"}
    </Button>
  );
}
