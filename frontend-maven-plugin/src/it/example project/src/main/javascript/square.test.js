import { expect } from "@esm-bundle/chai";
import { square } from "./square.js";

describe("The square function", function () {
  it("should square a number", function () {
    expect(square(3)).to.eql(9);
  });
});
