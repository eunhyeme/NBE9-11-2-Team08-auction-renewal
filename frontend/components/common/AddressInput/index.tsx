"use client";

import { useState, useRef, useEffect } from "react";
import Script from "next/script";
import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

declare global {
  interface Window {
    daum: {
      Postcode: new (options: {
        oncomplete: (data: { roadAddress: string; zonecode: string }) => void;
      }) => { open: () => void };
    };
  }
}

interface AddressInputProps {
  value?: string;
  onChange: (value: string) => void;
  error?: string;
}

export function AddressInput({ value, onChange, error }: AddressInputProps) {
  const [zipCode, setZipCode] = useState("");
  const [roadAddress, setRoadAddress] = useState("");
  const [detailAddress, setDetailAddress] = useState("");
  const initializedRef = useRef(false);

  // profile edit 등에서 기존 주소 초기화 (우편번호 파싱)
  useEffect(() => {
    if (!initializedRef.current && value) {
      const match = value.match(/^\[(\d{5})\] (.+?)(?:\s*\|\s*(.+))?$/);
      if (match) {
        setZipCode(match[1]);
        setRoadAddress(match[2]);
        setDetailAddress(match[3] ?? "");
      } else {
        setRoadAddress(value);
      }
      initializedRef.current = true;
    }
  }, [value]);

  const combine = (zip: string, road: string, detail: string) =>
    `[${zip}] ${road}${detail ? " | " + detail : ""}`;

  const handleSearch = () => {
    new window.daum.Postcode({
      oncomplete: (data) => {
        setZipCode(data.zonecode);
        setRoadAddress(data.roadAddress);
        setDetailAddress("");
        onChange(combine(data.zonecode, data.roadAddress, ""));
      },
    }).open();
  };

  const handleDetailChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const detail = e.target.value;
    setDetailAddress(detail);
    onChange(combine(zipCode, roadAddress, detail));
  };

  return (
    <div className="flex flex-col gap-2">
      <Script
        src="//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"
        strategy="lazyOnload"
      />
      <div className="flex gap-2">
        <Input
          value={zipCode}
          readOnly
          placeholder="우편번호"
          className="bg-muted cursor-default w-24 shrink-0"
        />
        <Button
          type="button"
          variant="outline"
          onClick={handleSearch}
          className="shrink-0"
        >
          <Search className="h-4 w-4 mr-1.5" />
          검색
        </Button>
      </div>
      <Input
        value={roadAddress}
        readOnly
        placeholder="도로명주소"
        className="bg-muted cursor-default"
      />
      <Input
        value={detailAddress}
        onChange={handleDetailChange}
        placeholder="상세주소 (동/호수 등)"
        disabled={!roadAddress}
      />
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}
