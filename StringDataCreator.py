
import random
import sys


def _next_in_list(l: list[int]):
    if len(l) > 0:
        return l.pop(0)
    else:
        return -1


def create_subfile(filename: str, new_filename: str, n: int):
    """
        creates a new file with the first `n` instances of `filename`
    """
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

    if (n > length):
        raise Exception("n is larger than the file length")

    for i in range(0, n + prefix_end):
        new_file.write(lines[i])


def mutate_string_arg(line: str, arg_nr: int):
    """
        mutates argument `arg_nr` in instance `line`.
        NB the argument at position `arg_nr` is assumed to be a string
    """
    split = line.split(", ")

    combo = split[:arg_nr-1] + \
        ["'new text here" + str(random.randint(0, 100000000)
                                ) + "'"] + split[arg_nr:]

    return ", ".join(combo)


def create_changed_file(insertions: int, deletions: int, filename: str, new_filename: str):
    """
        creates a new file where `insertions` instances are inserted and `deletions` instances are deleted.
    """
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
    insertion_line_numbers = random.sample(
        range(prefix_end, length), insertions)
    deletion_line_numbers = random.sample(range(prefix_end, length), deletions)
    insertion_line_numbers.sort()
    deletion_line_numbers.sort()
    print("Insertion line numbers: ", insertion_line_numbers)
    print("Deletion line numbers: ", deletion_line_numbers)

    next_insert_line = _next_in_list(insertion_line_numbers)
    next_delete_line = _next_in_list(deletion_line_numbers)
    for line_nr, line in enumerate(lines):
        # insert instance here
        if line_nr == next_insert_line:
            new_file.write(mutate_string_arg(line, 2))
            next_insert_line = _next_in_list(insertion_line_numbers)
        # delete instance here
        if line_nr == next_delete_line:
            next_delete_line = _next_in_list(deletion_line_numbers)
        # copy line to new file
        else:
            new_file.write(line)

    original_file.close()
    new_file.close()


def run(source_dir: str, source: str, target_dir: str, file_sizes: list[str], insert_nr: int, delete_nr: int):
    """
        for every N in `file_sizes`:
            create a new file with the first N instances of `source`
            create a new file with `insert_nr` insertions and `delete_nr` deletions
        the files are placed in `target_dir`
    """
    for size in file_sizes:
        create_subfile(source_dir + source,
                       target_dir + size + "_old_" + source, int(size))

        create_changed_file(insert_nr, delete_nr,
                            target_dir + size + "_old_" + source, target_dir + size + "_new_" + source)


if __name__ == "__main__":
    if len(sys.argv) < 4:
        raise Exception(
            "Not enough arguments. Run with arguments <file sizes (n)> <number of insertions> <number of deletions>")

    file_sizes = sys.argv[1].split(", ")
    delete_nr = int(sys.argv[2])
    insert_nr = int(sys.argv[3])

    source_dir = "temp/"
    source = "exoplanets.stottr"
    target_dir = "temp/generated/"

    run(source_dir, source, target_dir,
        file_sizes, insert_nr, delete_nr)
