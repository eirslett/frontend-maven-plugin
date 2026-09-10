import { expect, test } from "vitest";
import { square } from "./square.js";

test("squares a number", () => {
  expect(square(3)).toBe(9);
});
