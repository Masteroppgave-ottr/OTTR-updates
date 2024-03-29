
import random
import sys

from TestDataCreator import insert

tag = "\033[93m[CREATE]\033[0m"


def _next_in_list(l: list[int]):
    if len(l) > 0:
        return l.pop(0)
    else:
        return -1


def _find_prefix_end(lines: list[str]):
    prefix_end = 0
    for nr, line in enumerate(lines):
        if len(line) > 1 and not line.startswith("@"):
            prefix_end = nr
            break
    return prefix_end


def create_copy_of_length(filename: str, new_filename: str, n: int):
    """
        creates a new file with the first `n` instances of `filename` If n is larger than the number of instances in `filename`, new random instances are generated.
    """
    print(f"{tag} creating shortened file", new_filename)
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
        print(f"{tag} generating", n - length, "new instances")
        for i in range(prefix_end, n - length):
            instance = lines[random.randint(prefix_end, length-1)]
            new_file.write(mutate_instance_argument_n(
                instance, 1, rng_range=10000000000000000000))


def mutate_instance_argument_n(instance: str, arg_nr: int, new_value: str = None, rng_range: int = 10000000000000000000):
    front = instance[:instance.find("(")+1]
    end = instance[instance.rfind(")"):]
    arguments = instance[instance.find("(")+1:instance.rfind(")")].split(", ")

    if arg_nr > len(arguments):
        raise Exception(
            f"you are trying to mutate argument {arg_nr} of an instance with only {len(arguments)} arguments")

    if new_value is None:
        new_value = '<http://example.org/newID' + \
            str(random.randint(0, rng_range)) + '>'

    mutated_instance = front
    for i in range(0, len(arguments)):
        if i == arg_nr-1:
            mutated_instance += new_value + ", "
        else:
            mutated_instance += arguments[i] + ", "

    mutated_instance = mutated_instance[:-2] + end
    return mutated_instance


def add_n_half_duplicates(filename: str, n: str):
    file = open(filename)
    lines = file.readlines()
    length = len(lines)
    prefix_end = _find_prefix_end(lines)

    # create a list of n line numbers
    mutate_line_numbers = random.sample(range(prefix_end, length), n)

    line_nr_new_instance_pairs = []
    for i in range(0, len(mutate_line_numbers)):
        line = lines[mutate_line_numbers[i]]
        newLine = mutate_instance_argument_n(
            line, 3, f"<http://example.org/possibleDuplicateID/{i}>")
        newLine = mutate_instance_argument_n(
            newLine, 4, f'"possibleSun{i}"')
        lines[mutate_line_numbers[i]] = newLine
        line_nr_new_instance_pairs.append((mutate_line_numbers[i], newLine))

    # write the new lines to the file
    file = open(filename, "w")
    file.writelines(lines)
    file.close()
    return line_nr_new_instance_pairs


def add_n_duplicates(filename: str, n: int, seed: str = ""):
    """
        adds n duplicates to the file
        returns the paris of line numbers and the new instances
    """
    file = open(filename)
    lines = file.readlines()
    length = len(lines)
    prefix_end = _find_prefix_end(lines)

    # create a list of n pairs of line numbers, these will be duplicates
    mutate_line_numbers = random.sample(range(prefix_end, length), n*2)

    line_nr_new_instance_pairs = []
    # iterate 2 and 2 elements from line_numbers
    for i in range(0, len(mutate_line_numbers), 2):
        line1 = lines[mutate_line_numbers[i]]
        newLine1 = mutate_instance_argument_n(
            line1, 3, f"<http://example.org/duplicateID/{seed}{i}>")
        newLine1 = mutate_instance_argument_n(
            newLine1, 4, f'"sun{seed}{i}"')

        line2 = lines[mutate_line_numbers[i+1]]
        newLine2 = mutate_instance_argument_n(
            line2, 3, f"<http://example.org/duplicateID/{seed}{i}>")
        newLine2 = mutate_instance_argument_n(
            newLine2, 4, f'"sun{seed}{i}"')

        lines[mutate_line_numbers[i]] = newLine1
        lines[mutate_line_numbers[i+1]] = newLine2
        line_nr_new_instance_pairs.append((mutate_line_numbers[i], newLine1))
        line_nr_new_instance_pairs.append((mutate_line_numbers[i+1], newLine2))

    # write the new lines to the file
    file = open(filename, "w")
    file.writelines(lines)
    file.close()
    return line_nr_new_instance_pairs


def add_n_blanks(filename: str, n: int, seed: str = ""):
    file = open(filename)
    lines = file.readlines()
    length = len(lines)
    prefix_end = _find_prefix_end(lines)

    # create a list of n line numbers, these will contain blank nodes
    mutate_line_numbers = random.sample(range(prefix_end, length), n)

    added_blanks = []
    for i in range(0, len(mutate_line_numbers)):
        line = lines[mutate_line_numbers[i]]
        newLine = mutate_instance_argument_n(
            line, 3, f"_:blankID{seed}{i}")
        lines[mutate_line_numbers[i]] = newLine
        added_blanks.append((mutate_line_numbers[i], newLine))

    # write the new lines to the file
    file = open(filename, "w")
    file.writelines(lines)
    file.close()
    return added_blanks


def create_file_nInstances(deletions: int, changes: int, insertions: int, filename: str, new_filename: str):
    """
        creates a new file where `insertions` instances are inserted and `deletions` instances are deleted.
    """
    print(
        f"{tag} creating changed   file  {new_filename} del[{deletions}] change[{changes}] insert[{insertions}]")

    # open source and target file
    original_file = open(filename, "r")
    new_file = open(new_filename, "w")
    lines = original_file.readlines()
    length = len(lines)
    prefix_end = _find_prefix_end(lines)

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
            new_file.write(mutate_instance_argument_n(line, 1))
            next_change_line = _next_in_list(change_line_numbers)

        # insert instance
        if line_nr == next_insert_line:
            while line_nr == next_insert_line:
                new_file.write(mutate_instance_argument_n(line, 1))
                next_insert_line = _next_in_list(insert_line_numbers)

    original_file.close()
    new_file.close()


def run_nInstances(source_dir: str, source: str, target_dir: str, file_sizes: list[str], delete_nr: int, change_nr: int, insert_nr: int, duplicate_insert_nr: int, duplicate_delete_nr: int, blank_insert_nr: int, blank_delete_nr: int):
    """
        for every N in `file_sizes`:
            create a new file with the first N instances of `source`
            create a new file with `insert_nr` insertions and `delete_nr` deletions
        the files are placed in `target_dir`
    """
    for size in file_sizes:
        create_copy_of_length(source_dir + source,
                              target_dir + size + "_old_" + source, int(size))

        create_file_nInstances(delete_nr, change_nr, insert_nr, target_dir +
                               size + "_old_" + source, target_dir + size + "_new_" + source)

        # add duplicates and blank nodes if specified
        if duplicate_insert_nr > 0:
            add_n_duplicates(target_dir + size + "_new_" +
                             source, duplicate_insert_nr, "insert")
        if duplicate_delete_nr > 0:
            add_n_duplicates(target_dir + size + "_old_" +
                             source, duplicate_delete_nr, "delete")

        if blank_insert_nr > 0:
            add_n_blanks(target_dir + size + "_new_" +
                         source, blank_insert_nr, "insert")
        if blank_delete_nr > 0:
            add_n_blanks(target_dir + size + "_old_" +
                         source, blank_delete_nr, "delete")


def create_file_nChanges(source_dir: str, source: str, target_dir: str, file_size: int, deletions: list[str], changes: list[str], insertions: list[str]):
    """
        for every N in `changes`:
            create a new file with the first `file_size` instances of `source`
            create a new file with `N` changes
        the files are placed in `target_dir`
    """
    old_file_name = target_dir + str(file_size) + "_old_" + source
    create_copy_of_length(source_dir + source,
                          old_file_name, file_size)

    for i in range(len(deletions)):
        total_changes = int(deletions[i]) + \
            int(changes[i]) + int(insertions[i])

        create_file_nInstances(int(deletions[i]), int(changes[i]), int(insertions[i]), old_file_name,
                               target_dir + str(file_size) + "_changes_" + str(total_changes) + "_new_" + source)


def create_file_nDuplicates(source_dir: str, source: str, target_dir: str, file_size: int, deletions: list[str], insertions: list[str]):
    """
        for every N in `changes`:
            create a new file with the first `file_size` instances of `source`
            create a new file with `N` changes
        the files are placed in `target_dir`
    """
    old_file_name = target_dir + str(file_size) + "_old_" + source
    create_copy_of_length(source_dir + source,
                          old_file_name, file_size)

    # add one half of duplicates to the old file - by adding one more we create duplicates
    if (len(insertions) > 0 and int(insertions[len(insertions)-1]) > 0):
        half_duplicates = add_n_half_duplicates(
            old_file_name, int(insertions[len(insertions)-1]))

    # add duplicates to the old file - this will be deleted later
    if (len(deletions) > 0 and int(deletions[len(deletions)-1]) > 0):
        line_dup_instance_pairs = add_n_duplicates(
            old_file_name, int(deletions[len(deletions)-1]))
        delete_line_numbers = []
        for i in range(0, len(line_dup_instance_pairs), 2):
            delete_line_numbers.append(line_dup_instance_pairs[i][0])
        delete_line_numbers.sort(reverse=True)

    for i in range(len(deletions)):
        file = open(old_file_name)
        lines = file.readlines()
        changes = int(deletions[i]) + int(insertions[i])

        # delete the correct number of duplicates
        if (len(deletions) > 0 and int(deletions[i]) > 0):
            for j in range(int(deletions[i])):
                line_to_delete = int(delete_line_numbers[j])
                inst = lines.pop(line_to_delete)

        if (len(insertions) > 0 and int(insertions[i]) > 0):
            for k in range(int(insertions[i])):
                line_to_insert = half_duplicates[k][1]
                lines.append(mutate_instance_argument_n(
                    line_to_insert, 1, f"<http://example.org/uniqueID/{k}>"))

        # write the new file
        new_file = open(target_dir + str(file_size) +
                        "_changes_" + str(changes) + "_new_" + source, "w")
        new_file.writelines(lines)
        new_file.close()
        print(tag + "created file", target_dir + str(file_size) +
              "_changes_" + str(changes) + "_new_" + source)

    file.close()


def create_file_nBlanks(source_dir: str, source: str, target_dir: str, file_size: int, deletions: list[str], insertions: list[str]):
    """
        for every N in `changes`:
            create a new file with the first `file_size` instances of `source`
            create a new file with `N` changes
        the files are placed in `target_dir`
    """
    old_file_name = target_dir + str(file_size) + "_old_" + source
    create_copy_of_length(source_dir + source,
                          old_file_name, file_size)

    # add blanks to the old file - this will be deleted later
    if (len(deletions) > 0 and int(deletions[len(deletions)-1]) > 0):
        added_blanks = add_n_blanks(
            old_file_name, int(deletions[len(deletions)-1]))
        added_blanks.sort(key=lambda x: x[0], reverse=True)

    for i in range(len(deletions)):
        file = open(old_file_name)
        lines = file.readlines()
        prefix_end = _find_prefix_end(lines)
        changes = int(deletions[i]) + int(insertions[i])

        # remove blank nodes that was added earlier
        if (len(deletions) > 0 and int(deletions[i]) > 0):
            for j in range(int(deletions[i])):
                lines.pop(added_blanks[j][0])

            # add blank nodes to be inserted

        if (len(insertions) > 0 and int(insertions[i]) > 0):
            for k in range(int(insertions[i])):
                line_to_modify = lines[random.randint(
                    prefix_end, len(lines)-1)]
                line_to_modify = mutate_instance_argument_n(
                    line_to_modify, 3, "_:blank" + str(k))
                lines.append(mutate_instance_argument_n(line_to_modify, 1))

            # write the new file
        new_file = open(target_dir + str(file_size) +
                        "_changes_" + str(changes) + "_new_" + source, "w")
        new_file.writelines(lines)
        new_file.close()
        print(tag + "created file", target_dir + str(file_size) +
              "_changes_" + str(changes) + "_new_" + source)

    file.close()


if __name__ == "__main__":
    source_dir = sys.argv[2]
    target_dir = source_dir+"generated/"

    mode = sys.argv[1]
    if (mode == "n=instances"):
        if len(sys.argv) < 8:
            raise Exception(
                "Not enough arguments. Run with arguments: n=instances <instance_file> <list of number of instances> <number of deletions> <number of changes> <number of insertions>")

        source = sys.argv[3]
        file_sizes = sys.argv[4].split(", ")
        delete_nr = int(sys.argv[5])
        change_nr = int(sys.argv[6])
        insert_nr = int(sys.argv[7])
        duplicate_insert_nr = int(sys.argv[8])
        duplicate_delete_nr = int(sys.argv[9])
        blank_insert_nr = int(sys.argv[10])
        blank_delete_nr = int(sys.argv[11])

        run_nInstances(source_dir, source, target_dir,
                       file_sizes, delete_nr, change_nr, insert_nr, duplicate_insert_nr, duplicate_delete_nr, blank_insert_nr, blank_delete_nr)

    if (mode == "n=changes"):
        if len(sys.argv) < 8:
            raise Exception(
                "Not enough arguments. Run with arguments: n=changes <instance_file> <number_of_instances> <list of deletions> <list of changes> <list of insertions>")

        source = sys.argv[3]
        file_size = int(sys.argv[4])
        nr_of_deletions = sys.argv[5].split(", ")
        nr_of_changes = sys.argv[6].split(", ")
        nr_of_insertions = sys.argv[7].split(", ")

        if (len(nr_of_deletions) != len(nr_of_changes) or len(nr_of_changes) != len(nr_of_insertions)):
            raise Exception(
                "Number of deletions, changes and insertions must be equal")

        create_file_nChanges(source_dir, source, target_dir,
                             file_size, nr_of_deletions, nr_of_changes, nr_of_insertions)

    if (mode == "n=duplicates"):
        if len(sys.argv) < 7:
            raise Exception(
                "Not enough arguments. Run with arguments: n=changes <instance_file> <number_of_instances> <list of dup deletions> <list of dup insertions>")

        source = sys.argv[3]
        file_size = int(sys.argv[4])
        nr_of_deletions = sys.argv[5].split(", ")
        nr_of_insertions = sys.argv[6].split(", ")
        if (len(nr_of_deletions) != len(nr_of_insertions)):
            raise Exception(
                "Number of deletions and insertions must be equal")

        create_file_nDuplicates(source_dir, source, target_dir,
                                file_size, nr_of_deletions, nr_of_insertions)

    if (mode == "n=blanks"):
        if len(sys.argv) < 7:
            raise Exception(
                "Not enough arguments. Run with arguments: n=changes <instance_file> <number_of_instances> <list of dup deletions> <list of dup insertions>")

        source = sys.argv[3]
        file_size = int(sys.argv[4])
        nr_of_deletions = sys.argv[5].split(", ")
        nr_of_insertions = sys.argv[6].split(", ")
        if (len(nr_of_deletions) != len(nr_of_insertions)):
            raise Exception(
                "Number of deletions and insertions must be equal")

        create_file_nBlanks(source_dir, source, target_dir,
                            file_size, nr_of_deletions, nr_of_insertions)
