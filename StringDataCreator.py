
import random
import sys

from TestDataCreator import insert


def _next_in_list(l: list[int]):
    if len(l) > 0:
        return l.pop(0)
    else:
        return -1


def create_subfile(filename: str, new_filename: str, n: int):
    """
        creates a new file with the first `n` instances of `filename`
    """
    print("[CREATE] creating shortened file", new_filename)
    # open a new file to write the data to
    original_file = open(filename, "r")
    new_file = open(new_filename, "w")

    lines = original_file.readlines()
    length = len(lines)
    prefix_end = 0
    for nr, line in enumerate(lines):
        if len(line) > 1 and not line.startswith("@"):
            prefix_end = nr
            break

    for i in range(0, min(n + prefix_end, length)):
        new_file.write(lines[i])

    if (n > length):
        print("[CREATE] generating", n - length, "new instances")
        for i in range(prefix_end, n - length):
            new_file.write(mutate_instance(
                lines[prefix_end+1], 2, rng_range=10000000000000000000))


def mutate_instance(instance: str, arg_nr: int, new_value: str = None, rng_range: int = 100000000):
    """
        mutates argument `arg_nr` in instance `line`.
        NB the argument at position `arg_nr` is assumed to be a string
    """
    if new_value is None:
        new_value = "'new text here" + str(random.randint(0, rng_range)) + "'"

    split = instance.split(", ")

    combo = split[:arg_nr-1] + [new_value] + split[arg_nr:]

    return ", ".join(combo)


def create_changed_file(deletions: int, changes: int, insertions: int, filename: str, new_filename: str):
    """
        creates a new file where `insertions` instances are inserted and `deletions` instances are deleted.
    """
    print(
        f"[CREATE] creating changed   file  {new_filename} del[{deletions}] change[{changes}] insert[{insertions}]")

    # open source and target file
    original_file = open(filename, "r")
    new_file = open(new_filename, "w")
    lines = original_file.readlines()
    length = len(lines)
    prefix_end = 0
    for nr, line in enumerate(lines):
        if len(line) > 1 and not line.startswith("@"):
            prefix_end = nr
            break

    # find line number to delete and insert
    change_delete_line_numbers = random.sample(
        range(prefix_end, length), deletions + changes)
    change_line_numbers = change_delete_line_numbers[:changes]
    deletion_line_numbers = change_delete_line_numbers[changes:]
    insert_line_numbers = [random.randint(
        prefix_end, length-1) for _ in range(insertions)]
    change_line_numbers.sort()
    deletion_line_numbers.sort()
    insert_line_numbers.sort()

    next_change_line = _next_in_list(change_line_numbers)
    next_delete_line = _next_in_list(deletion_line_numbers)
    next_insert_line = _next_in_list(insert_line_numbers)
    for line_nr, line in enumerate(lines):

        # copy line to new file
        if line_nr != next_delete_line and line_nr != next_change_line:
            new_file.write(line)

        # delete instance
        if line_nr == next_delete_line:
            next_delete_line = _next_in_list(deletion_line_numbers)

        # change instance
        if line_nr == next_change_line:
            new_file.write(mutate_instance(line, 2))
            next_change_line = _next_in_list(change_line_numbers)

        # insert instance
        if line_nr == next_insert_line:
            while line_nr == next_insert_line:
                new_file.write(mutate_instance(line, 2))
                next_insert_line = _next_in_list(insert_line_numbers)

    original_file.close()
    new_file.close()


def run(source_dir: str, source: str, target_dir: str, file_sizes: list[str], delete_nr: int, change_nr: int, insert_nr: int):
    """
        for every N in `file_sizes`:
            create a new file with the first N instances of `source`
            create a new file with `insert_nr` insertions and `delete_nr` deletions
        the files are placed in `target_dir`
    """
    for size in file_sizes:
        create_subfile(source_dir + source,
                       target_dir + size + "_old_" + source, int(size))

        create_changed_file(delete_nr, change_nr, insert_nr, target_dir +
                            size + "_old_" + source, target_dir + size + "_new_" + source)


if __name__ == "__main__":
    if len(sys.argv) < 4:
        raise Exception(
            "Not enough arguments. Run with arguments <file sizes (n)> <number of changes> <number of deletions>")

    source = sys.argv[1]
    file_sizes = sys.argv[2].split(", ")
    delete_nr = int(sys.argv[3])
    change_nr = int(sys.argv[4])
    insert_nr = int(sys.argv[5])

    source_dir = "temp/"
    target_dir = "temp/generated/"

    run(source_dir, source, target_dir,
        file_sizes, delete_nr, change_nr, insert_nr)
