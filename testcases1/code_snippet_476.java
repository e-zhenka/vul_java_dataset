@SuppressWarnings("unchecked") // deserialization is unsafe
  public static CompoundOrdering<Object> instantiate(SerializationStreamReader reader)
      throws SerializationException {
    int n = reader.readInt();
    List<Comparator<Object>> comparators = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      comparators.add((Comparator<Object>) reader.readObject());
    }
    return new CompoundOrdering<>(comparators);
  }