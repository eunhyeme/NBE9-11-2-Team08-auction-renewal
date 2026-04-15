import { Alert, AlertDescription } from "@/components/ui/alert";
import { AlertCircle } from "lucide-react";

interface ErrorMessageProps {
  message?: string;
}

export function ErrorMessage({
  message = "오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
}: ErrorMessageProps) {
  return (
    <Alert variant="destructive">
      <AlertCircle className="h-4 w-4" />
      <AlertDescription>{message}</AlertDescription>
    </Alert>
  );
}
