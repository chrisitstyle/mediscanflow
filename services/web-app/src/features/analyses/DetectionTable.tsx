import type { AnalysisDetection } from "@/types/analysis";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

type DetectionTableProps = {
  detections: AnalysisDetection[];
};

export function DetectionTable({ detections }: DetectionTableProps) {
  if (detections.length === 0) {
    return (
      <div className="rounded-lg border border-dashed p-8 text-center">
        <h3 className="text-sm font-semibold">No detections</h3>
        <p className="mt-2 text-sm text-muted-foreground">
          The model did not return any detected regions for this analysis.
        </p>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-lg border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Label</TableHead>
            <TableHead>Confidence</TableHead>
            <TableHead>X</TableHead>
            <TableHead>Y</TableHead>
            <TableHead>Width</TableHead>
            <TableHead>Height</TableHead>
          </TableRow>
        </TableHeader>

        <TableBody>
          {detections.map((detection, index) => (
            <TableRow key={`${detection.label}-${index}`}>
              <TableCell className="font-medium">{detection.label}</TableCell>
              <TableCell>{(detection.confidence * 100).toFixed(2)}%</TableCell>
              <TableCell>{detection.x.toFixed(2)}</TableCell>
              <TableCell>{detection.y.toFixed(2)}</TableCell>
              <TableCell>{detection.width.toFixed(2)}</TableCell>
              <TableCell>{detection.height.toFixed(2)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
