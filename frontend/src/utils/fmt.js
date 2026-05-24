const formatter = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export const fmt = (n) => `EGP ${formatter.format(Number(n) || 0)}`;
